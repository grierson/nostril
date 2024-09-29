(ns nostril.send
  (:require
   [nostril.events :as events]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [relay-stream event]
  (s/put! relay-stream (json/write-value-as-string event)))

(defn unsubscribe [{:keys [stream subscription-id]}]
  (submit stream (events/close-request subscription-id)))

(defn fetch
  ([relay-config]
   (fetch relay-config {}))
  ([{:keys [stream subscription-id]} filter]
   (submit stream (events/request-event subscription-id filter))))
