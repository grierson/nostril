(ns nostril.core
  (:require
   [hashp.core]
   [nostril.event-handler :as event-handler]
   [nostril.relay :as relay]
   [tick.core :as t]))

(defn configurator
  "Setup system"
  [{:keys [event-handler]
    :or {event-handler (event-handler/make-atom-event-handler)}}]
  {:event-handler event-handler})

(comment
  (def system (configurator {}))
  (def event-handler (:event-handler system))
  (def relay-url "wss://relay.damus.io")
  (def relay (relay/connect-to-relay! relay-url))
  (def submission (relay/fetch-latest event-handler (t/clock) (:stream relay)))
  (count (event-handler/fetch-all event-handler)))
