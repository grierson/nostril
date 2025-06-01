(ns nostril.driven.playgroud
  (:require
   [aleph.http :as http]
   [hato.websocket :as ws]
   [jsonista.core :as json]
   [nostril.driven.relay :refer [read-event request-event]]))

(comment
  "Using Aleph and manifold"
  (def connection @(http/websocket-client "wss://relay.damus.io"))
  (http/websocket-close! connection))

(comment
  "Using Hato JDK11 websockets"
  (let [ws @(ws/websocket "wss://relay.damus.io"
                          {:on-message (fn [ws msg _last?]
                                         (let [foo (json/read-value (str msg))
                                               [event-type :as event] (read-event foo)]
                                           (println event)
                                           (if (= event-type "EOSE")
                                             (do
                                               (println "EOSE event recieved - closing subscription")
                                               (ws/close! ws))
                                             (println msg))))
                           :on-close   (fn [ws status reason]
                                         (println "WebSocket closed!"))})]
    (ws/send! ws (json/write-value-as-string (request-event (random-uuid) {:limit 10})))))
