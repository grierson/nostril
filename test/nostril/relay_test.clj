(ns nostril.relay-test
  (:require
   [hashp.core]
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is testing]]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.relay :as relay]
   [malli.generator :as mg]
   [nostril.types :as types]
   [jsonista.core :as json]
   [nostril.read :as read]
   [nostril.event-handler :as event-handler]))

(defmacro with-server [server & body]
  `(let [server# ~server]
     (try
       ~@body
       (finally
         (.close ^java.io.Closeable server#)
         (netty/wait-for-close server#)))))

(defmacro with-handler [handler & body]
  `(with-server (http/start-server ~handler {:port 8080 :shutdown-timeout 0})
     ~@body))

(defn echo-handler
  ([req] (echo-handler {} req))
  ([options req]
   (-> (http/websocket-connection req options)
       (d/chain' #(s/connect % %))
       (d/catch'
        (fn [^Throwable e]
          (println e)
          {})))))

(deftest submit-connection-test
  (with-handler echo-handler
    (let [relay-url "ws://localhost:8080"
          relay-stream @(relay/connect relay-url)
          event (relay/request-event {})]
      (is (true? @(relay/submit relay-stream event))))))

(deftest close-connection-test
  (with-handler echo-handler
    (let [relay-url "ws://localhost:8080"
          relay-stream @(relay/connect relay-url)]
      (is (true? @(relay/close relay-stream))))))

(deftest add-relay-test
  (testing "adds relay to relays"
    (with-handler echo-handler
      (let [relays (atom {})
            relay-url "ws://localhost:8080"
            _ @(relay/add-relay relays relay-url)]
        (is (true? (contains? @relays relay-url))))))

  (testing "closing relay stream removes relay from relays"
    (with-handler echo-handler
      (let [relays (atom {})
            relay-url "ws://localhost:8080"
            _ @(relay/add-relay relays relay-url)
            relay-stream (get-in @relays [relay-url :stream])
            _ (s/close! relay-stream)]
        (is (true? (empty? @relays)))))))

(deftest callback-test
  (testing "Raise event when Nostr event received"
    (let [[_event-type subscription-id :as eose-event] (mg/generate types/EoseEvent)
          event-handler (event-handler/make-atom-event-handler)
          _ (relay/callback event-handler (json/write-value-as-string eose-event))
          events (event-handler/fetch-all event-handler)
          first-event (first events)]
      (is (= first-event
             {:type :event-received
              :payload ["EOSE" subscription-id]})))))
