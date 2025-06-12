(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [aleph.http :as http]
   [clojure.core.async :as async]
   [clojure.pprint :as pprint]
   [hato.websocket :as hato]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
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

(defmulti make-connection! :type)
(defmulti send! :type)
(defmulti close-connection! :type)

(defn consume [{:keys [websocket channel]}]
  (async/go-loop []
    (let [msg (async/<! channel)
          [event-type subscription-id :as event] (read-event (json/read-value (str msg)))]
      (if (= event-type "EOSE")
        (do
          (println "EOSE event recieved - closing subscription")
          (send! websocket (close-event subscription-id)))
        (do
          (println event)
          (recur))))))

(defrecord AtomRelayManager [relays]
  ports/RelayManager
  (connect! [_this url]
    (let [connection (make-connection! {:type :hato} url)]
      (swap! relays assoc url connection)))
  (disconnect! [_this url]
    (let [relay (get @relays url)]
      (close-connection! relay)
      (swap! relays dissoc url)))
  (subscribe! [_this url event]
    (let [relay (get @relays url)]
      (send! relay event)
      (consume relay))))

(defn make-atom-hashmap-relay-manager []
  (->AtomRelayManager (atom {})))

(defmethod make-connection! :hato [m url]
  (let [channel (async/chan)
        websocket @(hato/websocket
                    url
                    {:on-message (fn [_ws msg _last?] (async/put! channel msg))
                     :on-close   (fn [_ws _status _reason] (println "WebSocket closed!"))})]
    (-> m
        (assoc :channel channel)
        (assoc :websocket websocket))))

(defmethod send! :hato [{:keys [websocket]} event]
  (hato/send! websocket (json/write-value-as-string event)))

(defmethod close-connection! :hato [{:keys [websocket]}]
  (hato/close! websocket))

(comment
  "Using Hato websockets"
  (def hato-ws (make-connection! {:type :hato}  "wss://relay.damus.io"))
  (send! hato-ws (request-event (random-uuid) {:limit 10}))
  (consume hato-ws))

(defmethod make-connection! :aleph [m url]
  (let [channel (async/chan)
        websocket @(http/websocket-client url)
        _ (s/consume #(async/put! channel %) websocket)]
    (-> m
        (assoc :channel channel)
        (assoc :websocket websocket))))

(defmethod send! :aleph [{:keys [websocket]} event]
  (s/put! websocket (json/write-value-as-string event)))

(defmethod close-connection! :aleph [{:keys [websocket]}]
  (http/websocket-close! websocket))

(comment
  "Using Aleph websockets"
  (def aleph-ws (make-connection! {:type :aleph}  "wss://relay.damus.io"))
  (send! aleph-ws (request-event (random-uuid) {:limit 10}))
  (consume aleph-ws))

(comment
  (let [rm (make-atom-hashmap-relay-manager)
        url "wss://relay.damus.io"]
    (ports/connect! rm url)
    (ports/subscribe! rm url (request-event (random-uuid) {:limit 10}))))
