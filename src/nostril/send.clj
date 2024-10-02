(ns nostril.send
  (:require [nostril.client :as client]))

(defn request-event
  ([subscription-id]
   (request-event subscription-id {}))
  ([subscription-id filters]
   ["REQ" subscription-id filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn close [{:keys [stream subscription-id]}]
  (client/submit stream (close-event subscription-id)))

(defn fetch
  ([relay-config]
   (fetch relay-config {}))
  ([{:keys [stream subscription-id]} filter]
   (client/submit stream (request-event subscription-id filter))))
