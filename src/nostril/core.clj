(ns nostril.core
  (:require
   [hashp.core]
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]
   [malli.core :as m]
   [malli.transform :as mt]
   [malli.generator :as mg]))

(def hex-32 [:string {:min 64 :max 64}])
(def hex-64 [:string {:min 128 :max 128}])
(def Relay-url [:? uri?])
(def Timestamp [:int {:min 0 :max 9999999999999}])

(def TagE [:catn
           [:type [:= "e"]]
           [:event-id hex-32]
           [:relay-url Relay-url]])

(def TagP [:catn
           [:type [:= "p"]]
           [:event-id hex-32]
           [:relay-url Relay-url]])

(def TagA [:catn
           [:type [:= "a"]]
           [:event-rn hex-32]
           [:relay-url Relay-url]])

(def Event
  [:map
   [:id hex-32]
   [:pubkey hex-32]
   [:created_at Timestamp]
   [:kind [:int {:min 0 :max 65535}]]
   [:tags [:sequential [:alt TagA TagE TagP]]]
   [:content :string]
   [:sig hex-64]])

(def ResponseEvent
  [:catn
   [:type [:= "EVENT"]]
   [:subscription-id :string]
   [:event Event]])

(comment
  (mg/generate ResponseEvent))

(def client
  (try
    (http/websocket-client "wss://purplepag.es")
    (catch Exception e (println e))))

(def fetch-request
  (json/write-value-as-string
   ["REQ" "subid" {:kinds [1] :limit 10}]))

(def close-request
  (json/write-value-as-string
   ["CLOSE" "subid"]))

(defmulti read-event
  (fn [x]
    (let [[type & _] (json/read-value x)]
      type)))

(defmethod read-event "EVENT" [json-event]
  (let [event (json/read-value json-event json/keyword-keys-object-mapper)]
    (m/decode ResponseEvent event mt/string-transformer)))

(defmethod read-event "EOSE" [json-event]
  (println "eose")
  (println json-event))

(defmethod read-event :default [json-event]
  (println "default")
  (println json-event))

(comment
  (s/put! @client fetch-request)
  (s/put! @client close-request)
  (def temp-event (read-event @(s/take! @client ::drained)))
  (s/consume read-event @client))
