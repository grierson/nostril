(ns nostril.core
  (:require
   [clojure.core.async :as async]
   [hashp.core]
   [nostril.driven.event-store :as event-store]
   [nostril.driven.relay :as relay]
   [nostril.driving.humbleui :as humbleui]
   [nostril.driving.ports :as driving-ports]
   [nostril.util :as util]))

(defrecord Application [event-handler relays]
  driving-ports/DrivingPorts
  (for-add-relay! [_this url]
    (let [connection (relay/make-connection! {:ws-type :hato :url url})]
      (swap! relays assoc url connection)
      (relay/consume event-handler connection)))
  (for-send! [_this url event]
    (async/put! (get-in @relays [url :in-channel]) event)))

(defn make-application
  ([] (make-application {}))
  ([{:keys [event-store relays]
     :or {event-store (event-store/make-atom-event-store)
          relays (atom {})}}]
   (->Application event-store relays)))

(comment
  (make-application)
  (humbleui/make-app (make-application)))

(comment
  (def damus-url "wss://relay.damus.io")
  (def snort-url "wss://relay.snort.social")

  (def application (make-application))

  (driving-ports/for-add-relay! application damus-url)
  (driving-ports/for-send! application
                           damus-url
                           (relay/request-event {:since (- (util/now) 3600)
                                                 :until (util/now)
                                                 :limit 10}))
  (driving-ports/for-add-relay! application snort-url)
  (driving-ports/for-send! application
                           snort-url
                           (relay/request-event {:since (- (util/now) 3600)
                                                 :until (util/now)
                                                 :limit 10})))
