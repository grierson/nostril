(ns nostril.core
  (:require
   [hashp.core]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as ports]
   [nostril.driven.relay :as relay]))

(defn configurator
  "Setup system"
  [{:keys [event-handler relay-gateway]
    :or {event-handler (event-handler/make-atom-event-handler)
         relay-gateway (relay/->AlephRelayGateway)}}]
  {:event-handler event-handler
   :relay-gateway relay-gateway})

(comment
  (def system (configurator {}))
  (def event-handler (:event-handler system))
  (def relay-gateway (:relay-gateway system))
  (def relay-manager (relay/make-atom-hashmap-relay-manager event-handler relay-gateway))
  (def relay-url "wss://relay.damus.io")
  (def relay (ports/add-relay! relay-manager relay-url))
  (ports/get-relay relay-manager relay-url)
  (ports/submit! relay-manager relay-url (relay/request-event {}))
  (def events (ports/fetch-all event-handler))
  (take 10 (ports/fetch-all event-handler))
  (ports/remove-relay! relay-manager relay-url)
  (count (ports/fetch-all event-handler)))
