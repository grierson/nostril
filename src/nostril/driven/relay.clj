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
  (connect! [_this _url] stream)
  (close! [_this _relay-stream] (s/close! stream))
  (put! [_this _relay-stream event] (s/put! stream (json/write-value-as-string event))))

(defn make-inmemory-relay-gateway [] (->InMemoryRelayGateway (s/stream)))

(defrecord AlephRelayGateway []
  ports/RelayGateway
  (connect! [_this url]
    (try
      @(http/websocket-client url)
      (catch Exception e
        (println "Failed to connect to relay:" url)
        (throw e))))
  (close! [_this relay-stream] (http/websocket-close! relay-stream))
  (put! [_this stream event] (s/put! stream (json/write-value-as-string event))))

(defrecord Relay [url stream])

(defrecord AtomRelayManager [relays relay-gateway]
  ports/RelayManager
  (add-relay! [_this url]
    (let [relay-stream (ports/connect! relay-gateway url)
          relay (->Relay url relay-stream)]
      (swap! relays assoc url relay)))
  (get-relay [_this url]
    (get @relays url))
  (remove-relay! [_this url]
    (let [relay (get @relays url)]
      (ports/close! relay-gateway (:stream relay))
      (swap! relays dissoc url)))
  (submit! [_this url event]
    (let [relay (get @relays url)]
      (ports/put! relay-gateway (:stream relay) event))))

(defn make-atom-hashmap-relay-manager [relay-gateway]
  (->AtomRelayManager (atom {}) relay-gateway))

(comment
  (def event-handler (event-handler/make-atom-event-store))
  (def relay-manager (make-atom-hashmap-relay-manager (->AlephRelayGateway)))
  (def relay-url "wss://relay.damus.io")
  (ports/add-relay! relay-manager relay-url)
  (ports/put! relay-manager relay-url (request-event {:limit 10}))
  (ports/fetch-all event-handler)
  (http/websocket-close! (:stream (ports/get-relay relay-manager relay-url)))
  ()
  (count (ports/fetch-all event-handler)))
