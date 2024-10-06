(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.read :as read]
   [buddy.core.bytes :as]))

(def snowden "npub1sn0wdenkukak0d9dfczzeacvhkrgz92ak56egt7vdgzn8pv2wfqqhrjdv9")

(def connections (atom {}))
(def events (atom {}))

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
   (request-event {:kinds [1]
                   :limit 10
                   :authors [""]}))
  (s/consume
   (fn [raw-event]
     (let [[type _subscription-id :as event] (read/handle raw-event)]
       (if (= type "EVENT")
         (swap! events assoc (:subscription-id event) event)
         false)))
   (get @connections "wss://relay.damus.io"))
  (count @events))
