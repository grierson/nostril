(ns core
  (:require
   [aleph.http :as http]
   [manifold.stream :as s]
   [jsonista.core :as j]
   [hashp.core]))

(def client
  (try
    (http/websocket-client "wss://purplepag.es")
    (catch Exception e (println e))))

(def fetch-request
  (j/write-value-as-string ["REQ" "subid" {:kinds [1] :limit 10}]))

(defmulti read-event
  (fn [x]
    (let [[type & _] (j/read-value x)]
      type)))

(defmethod read-event "EVENT" [raw-event]
  (println "event")

  (let [[_type _subscription-id body] (j/read-value raw-event j/keyword-keys-object-mapper)]
    (println body)))

(defmethod read-event "EOSE" [params]
  (println "eose")
  (println params))

(defmethod read-event :default [params]
  (println "default")
  (println params))

(comment
  (s/put! @client fetch-request)
  (read-event @(s/take! @client)))
