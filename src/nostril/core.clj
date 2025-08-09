(ns nostril.core
  (:require
   [clojure.core.async :as async]
   [hashp.core]
   [nostril.driven.event-store :as event-store]
   [nostril.driven.relay :as relay]
   [nostril.driving.ports :as driving-ports]
   [nostril.util :as util]))

(defrecord Application [event-handler relays]
  driving-ports/DrivingPorts
  (for-getting-relays [_this] @relays)
  (for-adding-event! [_this event]
    (event-store/add-event! event-handler event))
  (for-getting-events [_this] (event-store/fetch-all event-handler))
  (for-adding-relay! [this url]
    (let [connection (relay/make-connection! {:ws-type :hato :url url})]
      (swap! relays assoc url connection)
      (relay/consume (partial driving-ports/for-adding-event! this) connection)))
  (for-sending-event! [_this url event]
    (async/put! (get-in @relays [url :in-channel]) event)))

(defn make-application
  ([] (make-application {}))
  ([{:keys [event-store relays]
     :or {event-store (event-store/make-atom-event-store)
          relays (atom {})}}]
   (->Application event-store relays)))

(comment
  (def damus-url "wss://relay.damus.io")
  (def snort-url "wss://relay.snort.social")

  (def application (make-application))

  (driving-ports/for-adding-relay! application damus-url)
  (driving-ports/for-sending-event! application
                                    damus-url
                                    (relay/request-event {:since (- (util/now) 360000)
                                                          :until (util/now)
                                                          :limit 10}))
  (driving-ports/for-getting-events application)
  (driving-ports/for-adding-relay! application snort-url)
  (driving-ports/for-sending-event! application
                                    snort-url
                                    (relay/request-event {:since (- (util/now) 3600)
                                                          :until (util/now)
                                                          :limit 10})))
