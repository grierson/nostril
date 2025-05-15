(ns nostril.core
  (:require
   [hashp.core]
   [nostril.driving.ports :as driving-ports]
   [nostril.driven.ports :as driven-ports]
   [nostril.driven.relay :as relay]
   [nostril.driving.humbleui :as humbleui]
   [nostril.util :as util]
   [nostril.driven.event-handler :as event-handler]))

(defn configurator
  "Setup driven components"
  [{:keys [relay-gateway]
    :or {relay-gateway (relay/->AlephRelayGateway)}}]
  (let [event-handler (event-handler/make-atom-event-store)]
    {:event-handler event-handler
     :relay-gateway relay-gateway}))

(defrecord Nostril [driven]
  driving-ports/DrivingPorts
  (get-events [_this]
    (driven-ports/fetch-all (:event-handler driven))))

(defn make-application []
  (let [application (configurator {})
        system (->Nostril application)]
    system))

(comment
  (make-application)
  (humbleui/make-app (make-application)))

(comment
  (def system (configurator {}))
  (def event-handler (:event-handler system))
  (def relay-gateway (:relay-gateway system))
  (def relay-manager (relay/make-atom-hashmap-relay-manager event-handler relay-gateway))
  (def relay-url "wss://relay.damus.io")
  (def relay (driven-ports/add! relay-manager relay-url))
  (driven-ports/fetch relay-manager relay-url)
  (driven-ports/put!
   relay-manager
   relay-url
   (relay/request-event {:since (- (util/now) 3600)
                         :until (util/now)
                         :limit 10}))
  (def events (driven-ports/fetch-all event-handler))
  (take 10 (driven-ports/fetch-all event-handler))
  (driven-ports/remove! relay-manager relay-url)
  (count (driven-ports/fetch-all event-handler)))
