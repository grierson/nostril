(ns nostril.core
  (:require
   [aleph.http :as http]
   [manifold.stream :as s]
   [jsonista.core :as json]
   [hashp.core]
   [malli.core :as m]
   [malli.generator :as mg]
   [malli.transform :as mt]))

(def hex-32 [:string {:min 64 :max 64}])
(def hex-64 [:string {:min 128 :max 128}])
(def unix-timestamp
  [:and int? [:fn {:error/message "must be a 10-digit number"} #(<= 1000000000 % 9999999999)]])
(def Kind [:int {:min 0 :max 65535}])
(def Relay-url [:? {:decode :uri} uri?])

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
   [:created_at unix-timestamp]
   [:kind Kind]
   [:tags [:sequential [:alt TagA TagE TagP]]]
   [:content string?]
   [:sig hex-64]])

(comment
  (mg/generate Event))

(def ResponseEvent
  [:catn
   [:type [:= "EVENT"]]
   [:subscription-id string?]
   [:event Event]])

(comment
  (mg/generate Event))

(def client
  (try
    (http/websocket-client "wss://purplepag.es")
    (catch Exception e (println e))))

(def fetch-request
  (json/write-value-as-string
   ["REQ" "subid" {:kinds [1] :limit 10}]))

(defmulti read-event
  (fn [x]
    (let [[type & _] (json/read-value x)]
      type)))

(def uri-transformer
  (mt/transformer
   mt/string-transformer
   {:decode {:uri (fn [s] (java.net.URI. s))}}))

(defmethod read-event "EVENT" [raw-event]
  (let [plain-event (json/read-value raw-event json/keyword-keys-object-mapper)]
    (m/decode ResponseEvent
              plain-event
              (mt/transformer
               uri-transformer))))

(comment
  (read-event (json/write-value-as-string (mg/generate ResponseEvent))))

(defmethod read-event "EOSE" [params]
  (println "eose")
  (println params))

(defmethod read-event :default [params]
  (println "default")
  (println params))

(comment
  (s/put! @client fetch-request)
  (read-event @(s/take! @client ::drained))
  (s/consume read-event @client))
