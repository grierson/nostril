(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.send :as send]))

(def connections (atom {}))
(def events (atom []))

(defn request-event
  ([]
   (request-event {}))
  ([filters]
   ["REQ" (str (random-uuid)) filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn request
  ([connection events]
   (request connection events {}))
  ([connection events filters]
   (let [[_REQ _subscription-id _filters :as event] (request-event filters)]
     #p (client/submit connection event)
     #p @(s/consume (fn [x] (swap! events conj #p x)) connection))))

(comment
  (swap! connections client/connect "wss://relay.damus.io")
  (request (get @connections "wss://relay.damus.io") events {:limit 10})
  (count @events))

(comment
  (swap! connections client/connect "wss://purplepag.es")
  (send/request (get @connections "wss://purplepag.es") {:kinds [1]})
  (swap! events client/append (get @connections "wss://purplepag.es")))
