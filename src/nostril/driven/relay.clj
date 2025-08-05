(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [clojure.core.async :as async]
   [clojure.pprint :as pprint]
   [hato.websocket :as hato]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [hashp.core]
   [nostril.types :as types]))

(defn request-event
  ([] (request-event (random-uuid) {}))
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" (str subscription-id) (merge filters {:kinds [1]})]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn read-event [[event-type :as event]]
  (case event-type
    "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
    "NOTICE" (pprint/pprint event)
    "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
    event))

(defmulti make-connection! :ws-type)
(defmulti send! :ws-type)
(defmulti close-connection! :ws-type)

(defn consume [{:keys [websocket channel]}]
  (async/go-loop []
    (let
     [msg (async/<! channel)
      [event-type subscription-id :as event] (read-event (json/read-value (str msg)))]
      (if (= event-type "EOSE")
        (do
          (println "EOSE event recieved - closing subscription")
          (send! websocket (close-event subscription-id)))
        (do
          (println event)
          (recur))))))

(defmethod make-connection! :hato [{:keys [url] :as request}]
  (let [in-channel (async/chan)
        out-channel (async/chan)
        websocket @(hato/websocket
                    url
                    {:on-message (fn [_ws msg _last?] (async/put! out-channel msg))
                     :on-close   (fn [_ws _status _reason] (println "WebSocket closed!"))})]
    (async/go-loop []
      (when-let [event (async/<! in-channel)]
        (try
          (hato/send! websocket (json/write-value-as-string event))
          (catch Exception e (println e)))
        (recur)))
    (-> request
        (assoc :in-channel in-channel)
        (assoc :out-channel out-channel)
        (assoc :websocket websocket))))

(comment
  "Using Hato websockets"
  (def hato-ws (make-connection! {:ws-type :hato :url  "wss://relay.damus.io"}))
  (async/go
    (async/>! (:in-channel hato-ws) (request-event (random-uuid) {:limit 10})))
  (async/go-loop []
    (when-let [msg (async/<! (:out-channel hato-ws))]
      (println msg)
      (recur))))
