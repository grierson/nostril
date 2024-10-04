(ns nostril.core
  (:require
   [hashp.core]
   [nostril.client :as client]
   [nostril.util :as util]
   [manifold.stream :as s]))

(def connections (atom {}))
(def events (atom []))

(defn request-event
  ([]
   (request-event {}))
  ([filters]
   ["REQ" (str (random-uuid)) filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(comment
  (swap! connections client/connect "wss://relay.damus.io")
  (client/submit
   (get @connections "wss://relay.damus.io")
   (request-event {:limit 10}))
  (client/submit
   (get @connections "wss://relay.damus.io")
   (request-event {:limit 10
                   :until (util/now)
                   :since (- (util/now) 1000)}))
  (s/consume
   (fn [event] (prn event))
   (get @connections "wss://relay.damus.io"))
  (count @events))
