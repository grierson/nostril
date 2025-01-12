(ns nostril.relay
  (:require
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [nostril.read :as read]
   [nostril.event-handler :as event-handler]
   [tick.core :as t]
   [clojure.core :as c]))

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
   ["REQ" (str subscription-id) filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn callback
  [events raw-event]
  (let [[event-type :as event] (read/handle raw-event)]
    (when (contains? #{"EOSE" "NOTICE" "EVENT" "CLOSED" "OK"} event-type)
      (event-handler/raise events {:type :event-received
                                   :payload event}))))

(defn add-relay
  "Create relay connection and attach to main stream"
  [relays url]
  (let [stream @(connect url)
        relay {:url url
               :stream stream}]
    (swap! relays assoc url relay)
    (s/on-closed stream (fn [] (swap! relays dissoc url)))
    relays))

(comment
  (let [now (.getEpochSecond (t/instant (t/clock)))
        [event-type sub-id body :as event] (request-event {:kinds [1]
                                                           :since (- now 300)
                                                           :until now
                                                           :limit 10})]
    [event-type sub-id body event]))

(defn fetch-latest
  ([stream] (fetch-latest stream (t/clock)))
  ([stream clock] (fetch-latest stream clock 10))
  ([stream clock limit]
   (let [now (.getEpochSecond (t/instant clock))
         event (request-event {:kinds [1]
                               :since (- now 10000)
                               :until now
                               :limit limit})]
     (submit stream event)
     (s/consume (partial callback stream) stream))))

(comment
  (def relay-stream (connect "wss://relay.damus.io"))
  (def consumer (fetch-latest @relay-stream (t/clock) 2)))
