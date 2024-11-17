(ns nostril.relay
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(defn submit [stream event] (s/try-put! stream (json/write-value-as-string event) 1000 :timeout))
(defn connect [url] (http/websocket-client url))
(defn close [connection] (http/websocket-close! connection))

(defn add-relay [main relays url]
  (let [stream @(connect url)
        relay {:stream stream
               :since nil}]
    (swap! relays assoc url relay)
    (s/on-closed main (fn [] (swap! relays dissoc url)))
    (s/connect stream main {:upstream true
                            :downstream false
                            :description url})
    relays))
