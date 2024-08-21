(ns nostril.core
  (:require
   [aleph.http :as http]
   [manifold.stream :as s]
   [jsonista.core :as json]
   [hashp.core]
   [malli.generator :as mg]))

(def hex-32 [:string {:min 64 :max 64}])
(def hex-64 [:string {:min 128 :max 128}])
(def unix-timestamp
  [:and int? [:fn {:error/message "must be a 10-digit number"} #(<= 1000000000 % 9999999999)]])
(def Kind [:int {:min 0 :max 65535}])

(def TagE [:catn
           [:type [:= "e"]]
           [:event-id hex-32]
           [:relay-url [:? uri?]]])

(def TagP [:catn
           [:type [:= "p"]]
           [:event-id hex-32]
           [:relay-url [:? uri?]]])

(def TagA [:catn
           [:type [:= "a"]]
           [:event-rn hex-32]
           [:relay-url [:? string?]]])

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

(defmethod read-event "EVENT" [raw-event]
  (json/read-value #p raw-event json/keyword-keys-object-mapper))

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
