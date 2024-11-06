(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.read :as read]
   [manifold.deferred :as d]))

;; Should be 64 hex for authors filter
(def jack "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m")
(def jack-hex64 "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2")

(def connections (atom {}))
(def events (atom []))

(defn request-event
  ([] (request-event {}))
  ([filters] ["REQ" (str (random-uuid)) filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn callback [stream events raw-event]
  (let [[type subscription-id :as event] (read/handle raw-event)]
    (condp = type
      "EVENT" (swap! events conj event)
      "EOSE" (d/chain stream #(client/submit % (close-event subscription-id))))))

(comment
  (swap! connections client/connect "wss://relay.damus.io")
  (def sink (get @connections "wss://relay.damus.io"))
  (def submit
    (-> (client/submit
         sink
         (request-event {:kinds [1]
                         :limit 10
                         :authors [jack-hex64]}))
        (d/catch #(str "something unexpected submitting: " (.getMessage %)))))
  (def consumer
    (-> (s/consume (partial callback sink events) sink)
        (d/catch #(str "something unexpected consuming: " (.getMessage %)))))
  (count @events)
  (map first @events))
