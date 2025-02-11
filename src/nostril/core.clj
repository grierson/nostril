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
  (def relay (ports/connect! relay-gateway relay-url))
  (def submission (ports/fetch-all event-handler))
  (count (ports/fetch-all event-handler)))
