(ns nostril.core
  (:require
   [hashp.core]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.relay :as relay]
   [nostril.driving.humbleui :as humbleui]
   [nostril.driving.ports :as driving-ports]
   [nostril.util :as util]))

(defrecord Application [event-handler relays]
  driving-ports/DrivingPorts
  (for-add-relay! [_this url]
    (reset! relays (relay/connect! @relays url)))
  (for-send! [_this url event]
    (relay/subscribe! @relays url event))
  (for-get-events [_this]
    (println event-handler)))

(defn make-application []
  (let [event-handler (event-handler/make-atom-event-store)
        system (->Application event-handler (atom {}))]
    system))

(comment
  (make-application)
  (humbleui/make-app (make-application)))

(comment
  (def application (make-application))
  (def damus-url "wss://relay.damus.io")
  (def snort-url "wss://relay.snort.social")
  (driving-ports/for-add-relay! application damus-url)
  (driving-ports/for-add-relay! application snort-url)
  (driving-ports/for-send! application
                           damus-url
                           (relay/request-event {:since (- (util/now) 3600)
                                                 :until (util/now)
                                                 :limit 10}))
  (driving-ports/for-send! application
                           snort-url
                           (relay/request-event {:since (- (util/now) 3600)
                                                 :until (util/now)
                                                 :limit 10})))
