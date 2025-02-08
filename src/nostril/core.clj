(ns nostril.core
  (:require
   [hashp.core]
   [nostril.event-handler :as event-handler]
   [nostril.relay :as relay]
   [tick.core :as t]))

(defn setup
  "Setup event handler, relay store"
  []
  (let [event-handler (event-handler/make-atom-event-handler)
        relays (atom {})]
    {:event-handler event-handler
     :relays relays}))

(comment
  (def system (setup))
  (def event-handler (:event-handler system))
  (def relay-url "wss://relay.damus.io")
  (def relay (relay/connect-to-relay! relay-url))
  (def submission (relay/fetch-latest event-handler (t/clock) (:stream relay)))
  (count (event-handler/fetch-all event-handler)))
