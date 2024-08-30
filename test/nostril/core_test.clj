(ns nostril.core-test
  (:require
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is testing]]
   [hashp.core]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.core :as core]
   [nostril.read :as read]
   [nostril.types :as types]))

(deftest process-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate types/ResponseEvent)
          json-event (json/write-value-as-string expected)
          actual (read/process json-event)]
      (is (= expected actual)))))

(deftest submit-test
  (testing "submitting fetch request"
    (let [subscription-id (mg/generate :string)
          stream (s/stream)
          request (core/fetch-request subscription-id)
          _ (core/submit stream request)
          stream-event @(s/take! stream)
          map-event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= request map-event)))))

(deftest unsubscribe-test
  (testing "unsubscribe one relay"
    (let [relay "wss://sample.com"
          stream (s/stream)
          subscription-id (mg/generate :string)
          relays {relay {:stream stream
                         :subscription-id subscription-id}}
          updated-relays (core/unsubscribe relays relay)
          stream-event @(s/take! stream)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (core/close-request subscription-id) event))
      (is (empty? updated-relays))))

  (testing "only unsubscribe one relay when two exist"
    (let [relay1 "wss://sample.com"
          relay2 "wss://other"
          stream1 (s/stream)
          stream2 (s/stream)
          subscription-id (mg/generate :string)
          relays {relay1 {:stream stream1
                          :subscription-id subscription-id}
                  relay2 {:stream stream2
                          :subscription-id subscription-id}}
          updated-relays (core/unsubscribe relays relay2)
          stream-event @(s/take! stream2)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (core/close-request subscription-id) event))
      (is (some? (get updated-relays relay1)))
      (is (nil? (get updated-relays relay2))))))

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

(deftest subscription-test
  (with-handler echo-handler
    (let [subscription-id (mg/generate :string)
          relay-url "ws://localhost:8080"
          new-relays (core/subscribe {} [{:url  relay-url
                                          :subscription-id subscription-id}])]
      (is (some? (get new-relays relay-url))))))

(deftest fetch-test
  (let [subscription-id (mg/generate :string)
        stream (s/stream)
        relay-url "ws://sample.com"
        relays {relay-url {:stream stream
                           :subscription-id subscription-id}}
        _ (core/fetch (get relays relay-url))
        stream-event @(s/take! stream)
        event (json/read-value stream-event json/keyword-keys-object-mapper)]
    (is (= (core/fetch-request subscription-id) event))))

(deftest append-test
  (testing "add event to events"
    (let [[_ _ body :as event] (mg/generate types/ResponseEvent)
          json-event (json/write-value-as-string event)
          stream (s/stream)
          _ (s/put! stream json-event)
          events (core/append {} stream)]
      (is (= {(:id body) event} events))))
  (testing "don't add duplicate events to events"
    (let [[_ _ body :as event] (mg/generate types/ResponseEvent)
          json-event (json/write-value-as-string event)
          stream (s/stream)
          _ (s/put! stream json-event)
          events (core/append {(:id body) event} stream)]
      (is (= {(:id body) event} events)))))
