(comment "Everything related to interacting with Nostr relay")

(ns nostril.relay
  (:require
   [aleph.http :as http]
   [clojure.pprint :as pprint]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [manifold.stream :as s]
   [nostril.event-handler :as event-handler]
   [nostril.types :as types]
   [tick.core :as t]))

(defn now [clock] (t/with-clock clock (str (t/now))))

(defn submit! [stream event] (s/try-put! stream (json/write-value-as-string event) 1000 :timeout))
(defn connect! [url] (http/websocket-client url))
(defn close! [connection] (http/websocket-close! connection))

;; Should be 64 hex for authors filter
(def jack "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m")
(def jack-hex64 "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2")

(defn request-event
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" (str subscription-id) (merge filters {:kinds [1]})]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn read-event [event-json]
  (let [[event-type :as event] (json/read-value event-json json/keyword-keys-object-mapper)]
    (case event-type
      "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
      "NOTICE" (pprint/pprint event)
      "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
      event)))

(defn callback
  [event-store clock relay raw-event]
  (let [[event-type subscription-id :as event] (read-event raw-event)]
    (case event-type
      "EVENT"
      (event-handler/raise
       event-store
       {:id (random-uuid)
        :type :event-received
        :time (now clock)
        :data-content-type event-type
        :data event
        :source (:url relay)})

      "EOSE"
      (submit! (:stream relay) (close-event subscription-id))

      (println event))))

(defn connect-to-relay!
  [event-handler clock url]
  (let [stream @(connect! url)
        relay {:stream stream
               :url url}]
    (s/consume (partial callback event-handler clock relay) stream)
    relay))

(defn add-relay
  "Add relay to relays"
  [relays {:keys [url] :as relay}]
  (assoc relays url relay))

(comment
  (def event-handler (event-handler/make-atom-event-handler))
  (def relay (connect-to-relay! event-handler (t/clock) "wss://relay.damus.io"))
  (def consumer (submit! (:stream relay) (request-event {:limit 10})))
  (event-handler/fetch-all event-handler)
  (count (event-handler/fetch-all event-handler)))
