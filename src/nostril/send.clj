(ns nostril.send
  (:require
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn fetch-request [subscription-id]
  ["REQ" subscription-id {:kinds [1]}])

(defn close-request [subscription-id]
  ["CLOSE" subscription-id])

(defn submit [relay-stream event]
  (s/put! relay-stream (json/write-value-as-string event)))

(defn unsubscribe [{:keys [stream subscription-id]}]
  (submit stream (close-request subscription-id)))

(defn fetch [{:keys [stream subscription-id]}]
  (submit stream (fetch-request subscription-id)))
