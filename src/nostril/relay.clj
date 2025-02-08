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

(defn now [clock] (t/with-clock clock (t/now)))

(defn submit! [stream event] (s/try-put! stream (json/write-value-as-string event) 1000 :timeout))
(defn connect! [url] (http/websocket-client url))
(defn close! [connection] (http/websocket-close! connection))

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

(defn store-event!
  [event-store clock relay [event-type :as event]]
  (when (contains? #{"EVENT" "EOSE"} event-type)
    (event-handler/raise
     event-store
     {:id (random-uuid)
      :type :event-received
      :time (now clock)
      :data-content-type event-type
      :data event
      :source (:url relay)})))

(defn connect-to-relay! [url]
  (let [stream @(connect! url)
        relay {:stream stream
               :url url}]
    relay))

(defn consume-events-to-store! [clock event-handler relay]
  (s/consume
   (fn [raw-event] (store-event! event-handler clock relay (read-event raw-event)))
   (:stream relay)))

(defn add-relay
  "Add relay to relays"
  [relays {:keys [url] :as relay}]
  (assoc relays url relay))

(comment
  (def event-handler (event-handler/make-atom-event-handler))
  (def relay (connect-to-relay! "wss://relay.damus.io"))
  (consume-events-to-store! (t/clock) event-handler relay)
  (submit! (:stream relay) (request-event {:limit 10}))
  (event-handler/fetch-all event-handler)
  (count (event-handler/fetch-all event-handler)))
