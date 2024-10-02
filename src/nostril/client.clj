(ns nostril.client
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [connection event]
  (s/put! connection (json/write-value-as-string event)))

(defn create-connection [url] @(http/websocket-client url))

(defn close-connection [connection]
  (http/websocket-close! connection))

(defn connect [connections url]
  (if (get connections url)
    connections
    (assoc connections url (create-connection url))))
