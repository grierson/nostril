(ns nostril.store-test
  (:require
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.store :as store]
   [nostril.types :as types]))

(deftest append-test
  (testing "add event to events"
    (let [[_ _ body :as response-event] (mg/generate types/ResponseEvent)
          response-event-json (json/write-value-as-string response-event)
          stream (s/stream)
          _ (s/put! stream response-event-json)
          events (store/append {} stream)]
      (is (= {(:id body) response-event}
             events))))

  (testing "no duplicate events"
    (let [[_ _ body :as response-event] (mg/generate types/ResponseEvent)
          response-event-json (json/write-value-as-string response-event)
          stream (s/stream)
          _ (s/put! stream response-event-json)
          events (store/append {(:id body) response-event} stream)]
      (is (= {(:id body) response-event}
             events)))))

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
    (testing "add relay to relays"
      (let [subscription-id (mg/generate :string)
            relay-url "ws://localhost:8080"
            new-relays (store/subscribe {} {:url  relay-url
                                            :subscription-id subscription-id})]
        (is (some? (get new-relays relay-url)))))

    (testing "duplicates not added to relays"
      (let [subscription-id (mg/generate :string)
            relay-url "ws://localhost:8080"
            relays {relay-url {:stream (s/stream)
                               :subscription-id (mg/generate :string)}}
            new-relays (store/subscribe
                        relays
                        {:url  relay-url
                         :subscription-id subscription-id})]
        (is (= relays new-relays))))))
