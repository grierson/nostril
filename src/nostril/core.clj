(ns nostril.core
  (:require
   [hashp.core]
   [nostril.event-handler :as event-handler]
   [nostril.relay :as relay]
   [tick.core :as t]))

(defn setup
  "Setup event handler, relay store"
  []
  (let [handler (event-handler/make-atom-event-handler)
        relays (atom {})]
    {:handler handler
     :relays relays}))

(defn -main [& args]
  (let [{:keys [relays handler]} (setup)
        relay-url "wss://relay.damus.io"
        _ @(relay/add-relay relays relay-url)
        relay-stream (get @relays relay-url)
        _ @(relay/fetch-latest relay-stream)]
    (event-handler/fetch-all handler)))

(comment
  (def system (setup))
  (def handler (:handler system))
  (def relays (:relays system))
  (def relay-url "wss://relay.damus.io")
  @(relay/add-relay relays relay-url)
  (def relay-stream (get @relays relay-url))
  (:stream relay-stream)
  (def submission (relay/fetch-latest (t/clock) (:stream relay-stream)))
  (count (event-handler/fetch-all handler)))
