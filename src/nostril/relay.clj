(ns nostril.relay
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [stream event] (s/try-put! stream (json/write-value-as-string event) 1000 :timeout))
(defn connect [url] (http/websocket-client url))
(defn close [connection] (http/websocket-close! connection))
