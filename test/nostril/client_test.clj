(ns nostril.client-test
  (:require
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.send :as send]
   [nostril.store :as store]))

(deftest submit-test
  (testing "submits event as json"
    (let [subscription-id (mg/generate :string)
          stream (s/stream)
          request (send/request-event subscription-id)
          _ (client/submit stream request)
          stream-event (client/pull stream)
          map-event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (true? (string? stream-event)))
      (is (= request map-event)))))

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

(deftest close-connection-test
  (with-handler echo-handler
    (let [subscription-id (mg/generate :string)
          relay-url "ws://localhost:8080"
          new-relays (store/subscribe {} {:url  relay-url
                                          :subscription-id subscription-id})
          ws-conn (get new-relays relay-url)]
      (is (true? @(client/close-connection (:stream ws-conn)))))))
