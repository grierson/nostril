(ns nostril.driven.websocket
  (:require
   [aleph.http :as http]
   [clojure.core.async :as async]
   [hashp.core]
   [hato.websocket :as ws]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [nostril.driven.ports :refer [make-connection! RelayGateway send!]]
   [nostril.driven.relay :refer [close-event read-event request-event]]))

(defn consume [{:keys [channel connection]}]
  (async/go-loop []
    (let [msg (async/<! channel)
          [event-type subscription-id :as event] (read-event (json/read-value (str msg)))]
      (if (= event-type "EOSE")
        (do
          (println "EOSE event recieved - closing subscription")
          (send! connection (close-event subscription-id)))
        (do
          (println event)
          (recur))))))

(defrecord HatoWebsocket [channel connection]
  RelayGateway
  (make-connection! [this url]
    (let [channel (async/chan)
          websocket @(ws/websocket
                      url
                      {:on-message (fn [_ws msg _last?] (async/put! channel msg))
                       :on-close   (fn [_ws _status _reason] (println "WebSocket closed!"))})]
      (-> this
          (assoc :connection websocket)
          (assoc :channel channel))))
  (close-connection! [this] (ws/close! (:connection this)))
  (send! [this event]
    (ws/send! (:connection this) (json/write-value-as-string event))
    (consume this)))

(defn make-hato-ws [url]
  (let [ws (->HatoWebsocket nil nil)]
    (make-connection! ws url)))

(defrecord AlephRelayGateway [connection channel]
  RelayGateway
  (make-connection! [this url]
    (let [channel (async/chan)
          websocket @(http/websocket-client url)]
      (s/consume #(async/put! channel %) websocket)
      (-> this
          (assoc :connection websocket)
          (assoc :channel channel))))
  (close-connection! [this] (http/websocket-close! (:connection this)))
  (send! [this event] (s/put! (:connection this) (json/write-value-as-string event))))

(defn make-aleph-ws [url]
  (let [ws (->AlephRelayGateway nil nil)]
    (make-connection! ws url)))

(comment
  "Using Hato JDK11 websockets"
  (def hatos-ws (make-hato-ws "wss://relay.damus.io"))
  (send! hatos-ws (request-event (random-uuid) {:limit 10}))
  (consume hatos-ws))

(comment
  "Using Aleph websockets"
  (def aleph-ws (make-aleph-ws "wss://relay.damus.io"))
  (send! aleph-ws (request-event (random-uuid) {:limit 10}))
  (consume aleph-ws))
