(ns nostril.core
  (:require
   [hashp.core]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as driven-ports]
   [nostril.driven.relay :as relay]
   [nostril.driving.humbleui :as humbleui]
   [nostril.driving.ports :as driving-ports]
   [nostril.util :as util]))

(defn make-driven-components
  "Setup driven components"
  [{:keys [relay-manager relay-gateway event-handler]
    :or {relay-gateway (relay/->AlephRelayGateway)
         event-handler (event-handler/make-atom-event-store)}}]
  (let [relay-manager (or relay-manager (relay/make-atom-hashmap-relay-manager relay-gateway))]
    {:event-handler event-handler
     :relay-gateway relay-gateway
     :relay-manager relay-manager}))

(defrecord Application [event-handler relay-manager]
  driving-ports/DrivingPorts
  (for-add-relay! [_this url]
    (driven-ports/connect! relay-manager url))
  (for-close-relay! [_this url]
    (driven-ports/disconnect! relay-manager url))
  (for-send! [_this url event]
    (driven-ports/subscribe! relay-manager url event))
  (for-get-events [_this]
    (driven-ports/fetch-all event-handler)))

(defn make-application []
  (let [{:keys [event-handler relay-manager]} (make-driven-components {})
        system (->Application event-handler relay-manager)]
    system))

(comment
  (make-application)
  (humbleui/make-app (make-application)))

(comment
  (def system (make-driven-components {}))
  (def event-handler (:event-handler system))
  (def relay-gateway (:relay-gateway system))
  (def relay-manager (:relay-manager system))
  (def relay-url "wss://relay.damus.io")
  (driven-ports/add-relay! relay-manager relay-url)
  (driven-ports/subscribe!
   relay-manager
   relay-url
   (relay/request-event {:since (- (util/now) 3600)
                         :until (util/now)
                         :limit 10}))
  (def events (driven-ports/fetch-all event-handler))
  (take 10 (driven-ports/fetch-all event-handler)))
