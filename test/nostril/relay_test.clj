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
   [tick.core :as t]
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

(testing "relay gateway "
  (deftest submit-connection-test
    (with-handler echo-handler
      (let [relay-gateway (relay/->AlephRelayGateway)
            relay-url "ws://localhost:8080"
            relay (relay/connect! relay-gateway relay-url)
            event (relay/request-event {})]
        (is (true? @(relay/submit! relay-gateway (:stream relay) event))))))

  (deftest close-connection-test
    (with-handler echo-handler
      (let [relay-gateway (relay/->AlephRelayGateway)
            relay-url "ws://localhost:8080"
            relay (relay/connect! relay-gateway relay-url)]
        (is (true? @(relay/close! relay-gateway (:stream relay))))))))

(deftest add-relay-test
  (testing "adds relay to relays"
    (let [relay-url (mg/generate uri?)
          relay-stream (s/stream)
          relay (relay/map->Relay {:url relay-url
                                   :stream relay-stream})
          relay-manager (relay/make-atom-hashmap-relay-manager)
          relays (relay/add-relay relay-manager relay)]
      (is (contains? relays relay-url)))))

(deftest consume-test
  (testing "Raise event when Nostr event received"
    (let [[_event-type subscription-id :as nostr-event] (mg/generate types/ResponseEvent)
          json-nostr-event (json/write-value-as-string nostr-event)
          relay-url "ws://nostr.relay"
          relay (relay/map->Relay {:url relay-url
                                   :stream (s/stream)})
          event-handler (event-handler/make-atom-event-handler)
          relay-gateway (relay/->ManifoldRelayGateway)
          _ (relay/consume! relay-gateway event-handler relay)
          _ (s/put! (:stream relay) json-nostr-event)
          events (event-handler/fetch-all event-handler)
          first-event (first events)
          [_event-type event-subscription-id] #p (:data first-event)]
      (is (= subscription-id event-subscription-id)))))

;; (deftest read-test
;;   (testing "read EVENT event type"
;;     (let [expected (mg/generate types/ResponseEvent)
;;           response-event-json (json/write-value-as-string expected)
;;           actual (relay/read-event response-event-json)]
;;       (is (= actual expected))))
;;
;;   (testing "read EOSE event type"
;;     (let [expected (mg/generate types/EoseEvent)
;;           response-event-json (json/write-value-as-string expected)
;;           actual (relay/read-event response-event-json)]
;;       (is (= actual expected)))))
;;
;; (deftest request-event-test
;;   (testing "Default request event"
;;     (let [subscription-id (mg/generate :string)]
;;       (is (= ["REQ" subscription-id {:kinds [1]}]
;;              (relay/request-event subscription-id {})))))
;;   (testing "Cant override kinds"
;;     (let [subscription-id (mg/generate :string)]
;;       (is (= ["REQ" subscription-id {:kinds [1]}]
;;              (relay/request-event subscription-id {:kinds [2]})))))
;;   (testing "Include limit request event"
;;     (let [subscription-id (mg/generate :string)]
;;       (is (= ["REQ" subscription-id {:kinds [1] :limit 10}]
;;              (relay/request-event subscription-id {:limit 10}))))))
