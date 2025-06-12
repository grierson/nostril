(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [clojure.pprint :as pprint]
   [malli.core :as m]
   [malli.transform :as mt]
   [nostril.driven.ports :as ports]
   [nostril.types :as types]
   [nostril.driven.websocket :as websocket]))

(defn request-event
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" (str subscription-id) (merge filters {:kinds [1]})]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn read-event [[event-type :as event]]
  (case event-type
    "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
    "NOTICE" (pprint/pprint event)
    "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
    event))

(defrecord AtomRelayManager [relays]
  ports/RelayManager
  (connect! [_this url]
    (let [connection (websocket/make-connection! {:type :hato} url)]
      (swap! relays assoc url connection)))
  (disconnect! [_this url]
    (let [relay (get @relays url)]
      (websocket/close-connection! relay)
      (swap! relays dissoc url)))
  (subscribe! [_this url event]
    (let [relay (get @relays url)]
      (websocket/send! relay event)
      (websocket/consume relay))))

(defn make-atom-hashmap-relay-manager []
  (->AtomRelayManager (atom {})))

(comment
  (let [rm (make-atom-hashmap-relay-manager)
        url "wss://relay.damus.io"]
    (ports/connect! rm url)
    (ports/subscribe! rm url (request-event (random-uuid) {:limit 10}))))
