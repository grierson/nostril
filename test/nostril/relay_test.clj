(ns nostril.relay-test
  (:require
   [aleph.http :as http]
   [aleph.netty :as netty]
   [clojure.test :refer [deftest is]]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.relay :as relay]
   [nostril.core :refer [request-event]]))

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
          event (request-event {:kinds [1] :limit 10})]
      (is (true? @(relay/submit relay-stream event))))))

(deftest close-connection-test
  (with-handler echo-handler
    (let [relay-url "ws://localhost:8080"
          relay-stream @(relay/connect relay-url)]
      (is (true? @(relay/close relay-stream))))))
