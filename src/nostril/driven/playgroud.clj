(ns nostril.driven.playgroud
  (:require
   [aleph.http :as http]
   [clojure.core.async :as async]
   [hato.websocket :as ws]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [nostril.driven.ports :refer [make-connection! RelayGateway send!]]
   [nostril.driven.relay :refer [read-event request-event]]))

(defrecord AlephRelayGateway [connection]
  RelayGateway
  (make-connection! [_this url]
    (let [channel (async/chan)
          websocket @(http/websocket-client url)]
      (s/consume #(async/put! channel %) websocket)
      (reset! connection {:connection websocket
                          :channel channel})))
  (close-connection! [_this] (http/websocket-close! (get @connection :connection)))
  (send! [_this event] (s/put! (get @connection :connection) (json/write-value-as-string event))))

(defrecord HatoWebsocket [connection]
  RelayGateway
  (make-connection! [_ url]
    (let [channel (async/chan)
          websocket @(ws/websocket
                      url
                      {:on-message (fn [ws msg _last?]
                                     (let [foo (json/read-value (str msg))
                                           [event-type :as event] (read-event foo)]
                                       (if (= event-type "EOSE")
                                         (do
                                           (println "EOSE event recieved - closing subscription")
                                           (ws/close! ws))
                                         (async/put! channel event))))
                       :on-close   (fn [_ws _status _reason]
                                     (println "WebSocket closed!"))})]
      (reset! connection {:connection websocket
                          :channel channel})))
  (close-connection! [_] (ws/close! (get @connection :connection)))
  (send! [_ msg] (ws/send! (get @connection :connection) msg)))

(comment
  "Using Hato JDK11 websockets"
  (def ws (->HatoWebsocket (atom {})))
  (make-connection! ws "wss://relay.damus.io")
  (send! ws (json/write-value-as-string (request-event (random-uuid) {:limit 10})))
  (async/go-loop []
    (println (async/<! (get @(get ws :connection) :channel)))
    (recur)))

(comment
  "Using Aleph websockets"
  (def alephws (->AlephRelayGateway (atom {})))
  (make-connection! alephws "wss://relay.damus.io")
  (send! alephws (json/write-value-as-string (request-event (random-uuid) {:limit 10})))
  (async/go-loop []
    (println (async/<! (get @(get alephws :connection) :channel)))
    (recur)))
