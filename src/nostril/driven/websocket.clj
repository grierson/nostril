(ns nostril.driven.websocket
  (:require
   [aleph.http :as http]
   [clojure.core.async :as async]
   [hashp.core]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [nostril.driven.relay :refer [close-event read-event request-event]]))

(defmulti make-connection! :type)
(defmulti send! :type)
(defmulti close-connection! :type)

(defn consume [{:keys [websocket channel]}]
  (async/go-loop []
    (let [msg (async/<! channel)
          [event-type subscription-id :as event] (read-event (json/read-value (str msg)))]
      (if (= event-type "EOSE")
        (do
          (println "EOSE event recieved - closing subscription")
          (send! websocket (close-event subscription-id)))
        (do
          (println event)
          (recur))))))

(defmethod make-connection! :aleph [m url]
  (let [channel (async/chan)
        websocket @(http/websocket-client url)
        _ (s/consume #(async/put! channel %) websocket)]
    (-> m
        (assoc :channel channel)
        (assoc :websocket websocket))))

(defmethod send! :aleph [{:keys [websocket]} event]
  (s/put! websocket (json/write-value-as-string event)))

(defmethod close-connection! :aleph [{:keys [websocket]}]
  (http/websocket-close! websocket))

(comment
  "Using Aleph websockets"
  (def aleph-ws (make-connection! {:type :aleph}  "wss://relay.damus.io"))
  (send! aleph-ws (request-event (random-uuid) {:limit 10}))
  (consume aleph-ws))

