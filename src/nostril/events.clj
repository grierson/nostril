(ns nostril.events)

(defn request-event
  ([subscription-id]
   (request-event subscription-id {}))
  ([subscription-id filters]
   ["REQ" subscription-id filters]))

(defn close-request [subscription-id]
  ["CLOSE" subscription-id])
