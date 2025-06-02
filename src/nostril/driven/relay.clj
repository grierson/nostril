(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [aleph.http :as http]
   [clojure.pprint :as pprint]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [manifold.deferred :as deferred]
   [manifold.stream :as s]
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
  (send! [_this event] (s/put! stream (json/write-value-as-string event))))

(defn make-inmemory-relay-gateway
  ([] (->InMemoryRelayGateway (s/stream)))
  ([stream] (->InMemoryRelayGateway stream)))

(defrecord Relay [url stream])

(defrecord AtomRelayManager [relays relay-gateway]
  ports/RelayManager
  (connect! [_this url]
    (let [relay-stream (ports/make-connection! relay-gateway url)
          relay (->Relay url relay-stream)]
      (swap! relays assoc url relay)))
  (disconnect! [_this url]
    (let [relay (get @relays url)]
      (ports/disconnect! relay-gateway (:stream relay))
      (swap! relays dissoc url)))
  (subscribe! [_this url event]
    (let [relay (get @relays url)
          stream (:stream relay)
          [_ subscription-id _] event]
      (ports/send! relay-gateway event)
      (deferred/loop []
        (let [msg (s/take! stream)
              [event-type :as event] (-> msg
                                         (json/read-value json/keyword-keys-object-mapper)
                                         read-event)]
          (if (= event-type "EOSE")
            (do
              (println "EOSE event recieved - closing subscription")
              (ports/send! relay-gateway (close-event subscription-id))
              (ports/close-connection! relay-gateway))
            (do
              (println event)
              (deferred/recur))))))))

(defn make-atom-hashmap-relay-manager [relay-gateway]
  (->AtomRelayManager (atom {}) relay-gateway))
