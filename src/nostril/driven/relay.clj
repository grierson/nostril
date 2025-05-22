(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [aleph.http :as http]
   [clojure.pprint :as pprint]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [manifold.stream :as s]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as ports]
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

(defrecord InMemoryRelayGateway [stream]
  ports/RelayGateway
  (make-connection! [_this _url] stream)
  (close-connection! [_this _connection])
  (submit-relay! [_this _connection event] (s/put! stream (json/write-value-as-string event))))

(defn make-inmemory-relay-gateway
  ([] (->InMemoryRelayGateway (s/stream)))
  ([stream] (->InMemoryRelayGateway stream)))

(defrecord AlephRelayGateway []
  ports/RelayGateway
  (make-connection! [_this url]
    (try
      @(http/websocket-client url)
      (catch Exception e
        (println "Failed to connect to relay:" url)
        (throw e))))
  (close-connection! [_this connection] (http/websocket-close! connection))
  (submit-relay! [_this connection event] (s/put! connection  (json/write-value-as-string event))))

(defrecord Relay [url stream])

(defrecord AtomRelayManager [relays relay-gateway]
  ports/RelayManager
  (connect! [_this url]
    (let [relay-stream (ports/make-connection! relay-gateway url)
          relay (->Relay url relay-stream)]
      (swap! relays assoc url relay)))
  (disconnect! [_this url subscripition-id]
    (let [relay (get @relays url)]
      (ports/submit-relay! relay-gateway (:stream relay) (close-event subscripition-id))
      (ports/close-connection! relay-gateway (:stream relay))
      (swap! relays dissoc url)))
  (subscribe! [_this url event]
    (let [relay (get @relays url)
          stream (:stream relay)]
      (ports/submit-relay! relay-gateway stream event))))

(defn make-atom-hashmap-relay-manager [relay-gateway]
  (->AtomRelayManager (atom {}) relay-gateway))

(comment
  (def event-handler (event-handler/make-atom-event-store))
  (def relay-manager (make-atom-hashmap-relay-manager (->AlephRelayGateway)))
  (def relay-url "wss://relay.damus.io")
  (ports/fetch-all event-handler)
  (count (ports/fetch-all event-handler)))
