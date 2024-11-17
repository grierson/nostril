(ns nostril.relay
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [tick.core :as t]))

(defn submit [stream event] (s/try-put! stream (json/write-value-as-string event) 1000 :timeout))
(defn connect [url] (http/websocket-client url))
(defn close [connection] (http/websocket-close! connection))

;; Should be 64 hex for authors filter
(def jack "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m")
(def jack-hex64 "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2")

(defn request-event
  ([] (request-event (random-uuid) {}))
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" subscription-id filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn add-relay [main relays url]
  (let [stream @(connect url)
        relay {:stream stream}]
    (swap! relays assoc url relay)
    (s/on-closed main (fn [] (swap! relays dissoc url)))
    (s/connect stream main {:upstream true
                            :downstream false
                            :description url})
    relays))

(defn fetch-latest [clock {:keys [stream]}]
  (let [now (.getEpochSecond (t/instant clock))
        event (request-event {:kinds [1]
                              :since (- now 300)
                              :until now
                              :limit 10})]
    (submit stream event)))
