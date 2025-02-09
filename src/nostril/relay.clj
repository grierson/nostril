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
   [nostril.types :as types]))

(defn request-event
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" (str subscription-id) (merge filters {:kinds [1]})]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn read-event [[event-type :as event]]
  (case event-type
    "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
    "NOTICE" (pprint/pprint event)
    "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
    event))

(defprotocol RelayManager
  (add-relay [_this relay])
  (get-relay [_this url]))

(defrecord AtomRelayManager [relays]
  RelayManager
  (add-relay [_this {:keys [url] :as relay}]
    (swap! relays assoc url relay))
  (get-relay [_this url]
    (get @relays url)))

(defn make-atom-hashmap-relay-manager []
  (->AtomRelayManager (atom {})))

(defprotocol RelayGateway
  (connect! [this url])
  (submit! [this relay-stream event])
  (close! [this relay-stream])
  (consume! [_this event-handler relay]))

(defrecord Relay [url stream])

(defrecord ManifoldRelayGateway []
  RelayGateway
  (connect! [_this url]
    (let [stream (s/stream)
          relay (map->Relay {:stream stream :url url})]
      relay))
  (consume! [_this event-handler relay]
    (s/consume
     (fn [raw-event]
       (let [event (json/read-value raw-event json/keyword-keys-object-mapper)]
         (event-handler/raise event-handler (read-event event))))
     (:stream relay)))
  (submit! [_this relay-stream event]
    (s/try-put! relay-stream (json/write-value-as-string event) 1000 :timeout))
  (close! [_this relay-stream]
    (s/close! relay-stream)))

(defrecord AlephRelayGateway []
  RelayGateway
  (connect! [_this url]
    (let [stream @(http/websocket-client url)
          relay (map->Relay {:stream stream :url url})]
      relay))
  (consume! [_this event-handler relay]
    (s/consume
     (fn [raw-event]
       (let [event (json/read-value raw-event json/keyword-keys-object-mapper)]
         (event-handler/raise event-handler event)))
     (:stream relay)))
  (submit! [_this relay-stream event]
    (s/try-put! relay-stream (json/write-value-as-string event) 1000 :timeout))
  (close! [_this relay-stream]
    (http/websocket-close! relay-stream)))

(comment
  (def event-handler (event-handler/make-atom-event-handler))
  (def relay-gateway (->AlephRelayGateway))
  (def relay-url "wss://relay.damus.io")
  (connect! relay-gateway relay-url)
  (submit! relay-gateway relay-url (request-event {:limit 10}))
  (event-handler/fetch-all event-handler)
  (count (event-handler/fetch-all event-handler)))
