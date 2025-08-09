(comment "Everything related to interacting with Nostr relay")

(ns nostril.driven.relay
  (:require
   [clojure.core.async :as async]
   [clojure.pprint :as pprint]
   [hashp.core]
   [hato.websocket :as hato]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [nostril.driven.event-store :as event-store]
   [nostril.types :as types]))

(defn request-event
  ([] (request-event (random-uuid) {}))
  ([filters] (request-event (random-uuid) filters))
  ([subscription-id filters]
   ["REQ" (str subscription-id) (merge filters {:kinds [1] :limit 10})]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn read-event [[event-type :as event]]
  (case event-type
    "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
    "NOTICE" (pprint/pprint event)
    "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
    event))

(defmulti make-connection! :ws-type)
(defmulti close-connection! :ws-type)

(defmethod make-connection! :hato [{:keys [url] :as request}]
  (let [in-channel (async/chan)
        out-channel (async/chan 50)
        websocket @(hato/websocket
                    url
                    {:on-message (fn [_ws msg _last?] (async/put! out-channel msg))
                     :on-close   (fn [_ws _status _reason] (println "WebSocket closed!"))})]
    (async/go-loop []
      (when-let [event (async/<! in-channel)]
        (try
          (hato/send! websocket (json/write-value-as-string event))
          (catch Exception e (println "Error: " e)))
        (recur)))
    (-> request
        (assoc :in-channel in-channel)
        (assoc :out-channel out-channel)
        (assoc :websocket websocket))))

(defn consume [add-event-fn {:keys [in-channel out-channel]}]
  (async/go-loop []
    (let [msg (async/<! out-channel)
          [event-type subscription-id :as event] (read-event (json/read-value (str msg) json/keyword-keys-object-mapper))]
      (if (= event-type "EOSE")
        (do
          (println "EOSE event recieved - closing subscription")
          (async/>! in-channel (close-event subscription-id)))
        (do
          (println "Event: " event)
          (add-event-fn event)
          (recur))))))

(defmethod close-connection! :hato [{:keys [websocket in-channel out-channel]}]
  (hato/close! websocket)
  (async/close! in-channel)
  (async/close! out-channel))

(comment
  "Using Hato websockets"
  (def hato-damus-connection (make-connection! {:ws-type :hato :url  "wss://relay.damus.io"}))
  (def events-handler (event-store/make-atom-event-store))
  (async/>!! (:in-channel hato-damus-connection) (request-event))
  (consume events-handler hato-damus-connection)
  (close-connection! hato-damus-connection))
