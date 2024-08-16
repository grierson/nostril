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

(defmulti read-event (fn [x]
                       (let [[type & _] (j/read-value x)]
                         type)))

(defmethod read-event "EVENT" [params]
  (println params))

(defmethod read-event "EOSE" [params]
  (println params))

(defmethod read-event :default [params]
  (println params))

(comment
  (s/try-put! @client fetch-request 1000 :timeout)
  (j/rea) @(s/take! @client))
