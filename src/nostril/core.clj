(ns nostril.core
  (:require
   [nostril.send :as send]
   [nostril.store :as store]
   [hashp.core]))

(def relays (atom {}))
(def events (atom {}))

(comment
  (swap! relays store/subscribe [{:url "wss://relay.damus.io"
                                  :subscription-id "nostril-subid-damus"}
                                 {:url "wss://purplepag.es"
                                  :subscription-id "nostril-subid-purple"}])
  (send/fetch (get @relays "wss://purplepag.es"))
  (send/fetch (get @relays "wss://relay.damus.io"))
  (swap! events store/append (:stream (get @relays "wss://purplepag.es")))
  (count @events))
