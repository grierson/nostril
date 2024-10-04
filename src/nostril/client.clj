(ns nostril.client
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [connection event]
  (s/put! connection (json/write-value-as-string event)))

(defn close [connection]
  (http/websocket-close! connection))

(defn connect [connections url]
  (if (get connections url)
    connections
    (let [stream @(http/websocket-client url)]
      (assoc connections url stream))))
