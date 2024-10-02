(ns nostril.core
  (:require
   [nostril.send :as send]
   [nostril.store :as store]))

(def relays (atom {}))
(def events (atom {}))

(comment
  (swap! relays store/subscribe {:url "wss://relay.damus.io"
                                 :subscription-id "nostril-subid-damus"})
  (swap! relays store/subscribe {:url "wss://purplepag.es"
                                 :subscription-id "nostril-subid-purple"})
  (send/fetch (get @relays "wss://purplepag.es" {:kinds [1]}))
  (send/fetch (get @relays "wss://relay.damus.io") {:kinds [1]})
  (swap! events store/append (:stream (get @relays "wss://purplepag.es")))
  (swap! events store/append (:stream (get @relays "wss://relay.damus.io")))
  (count @events))
