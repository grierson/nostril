(ns nostril.client
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [relay-stream event]
  (s/put! relay-stream (json/write-value-as-string event)))

(defn pull [relay-stream] @(s/take! relay-stream))

(defn create-connection [{:keys [url subscription-id]}]
  {:stream @(http/websocket-client url)
   :url url
   :subscription-id subscription-id})

(defn close-connection [relay-stream]
  (http/websocket-close! relay-stream))
