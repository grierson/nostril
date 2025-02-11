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
  (close! [_this _relay-stream] (s/close! stream)))

(defrecord AlephRelayGateway []
  ports/RelayGateway
  (connect! [_this url]
    (try
      @(http/websocket-client url)
      (catch Exception e
        (println "Failed to connect to relay:" url)
        (throw e))))
  (close! [_this stream] (http/websocket-close! stream)))

(defrecord Relay [url stream])

(defrecord AtomRelayManager [relays event-handler relay-gateway]
  ports/RelayManager
  (add-relay! [_this url]
    (let [relay-stream (ports/connect! relay-gateway url)
          relay (->Relay url relay-stream)]
      (s/consume
       (fn [raw-event]
         (let [event (json/read-value raw-event json/keyword-keys-object-mapper)]
           (ports/raise! event-handler (read-event event))))
       relay-stream)
      (swap! relays assoc url relay)))
  (get-relay [_this url]
    (get @relays url))
  (remove-relay! [_this url]
    (ports/close! relay-gateway url)
    (swap! relays dissoc url))
  (submit! [_this url event]
    (let [relay (get @relays url)]
      (s/try-put! (:stream relay) (json/write-value-as-string event) 1000 :timeout))))

(defn make-atom-hashmap-relay-manager [event-handler relay-gateway]
  (->AtomRelayManager (atom {}) event-handler relay-gateway))

(comment
  (def event-handler (event-handler/make-atom-event-handler))
  (def relay-manager (make-atom-hashmap-relay-manager event-handler (->AlephRelayGateway)))
  (def relay-url "wss://relay.damus.io")
  (ports/submit! relay-manager relay-url (request-event {:limit 10}))
  (ports/fetch-all event-handler)
  (count (ports/fetch-all event-handler)))
