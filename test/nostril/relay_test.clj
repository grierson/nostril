(ns nostril.relay-test
  (:require
   [hashp.core]
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.core :refer [request-event]]
   [nostril.relay :as relay]
   [nostril.types :as types]
   [jsonista.core :as json]))

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
          event (request-event {})]
      (is (true? @(relay/submit relay-stream event))))))

(deftest close-connection-test
  (with-handler echo-handler
    (let [relay-url "ws://localhost:8080"
          relay-stream @(relay/connect relay-url)]
      (is (true? @(relay/close relay-stream))))))

(deftest add-relay-test
  (testing "adds relay to relays"
    (with-handler echo-handler
      (let [main (s/stream)
            relays (atom {})
            relay-url "ws://localhost:8080"
            new-relays @(relay/add-relay main relays relay-url)]
        (is (true? (contains? new-relays relay-url))))))

  (testing "connects relay to main"
    (with-handler echo-handler
      (let [main (s/stream)
            relays (atom {})
            relay-url "ws://localhost:8080"
            new-relays @(relay/add-relay main relays relay-url)
            relay-stream  (get-in new-relays [relay-url :stream])
            event  (request-event {})
            _  @(relay/submit relay-stream event)
            actual  @(s/take! main)]
        (is (= (json/write-value-as-string event) actual))))))
