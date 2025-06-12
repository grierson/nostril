(ns nostril.driven.relay-manager-test
  (:require
   [clojure.core.async :as async]
   [clojure.test :refer [deftest is testing use-fixtures]]
   [hashp.core]
   [hato.websocket :as hato]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [nostril.driven.relay :as relay :refer [request-event]]
   [nostril.types :as types]
   [org.httpkit.server :as http]))

(defn ws-handler-async-client [req]
  (http/as-channel
   req
   {:on-receive (fn [channel mesg] (http/send! channel mesg true))}))

(use-fixtures
  :once
  (fn [f]
    (let [server (http/run-server ws-handler-async-client {:port 4348})]
      (try (f) (finally (server))))))

(deftest test-websocket-ping-handler
  (let [url "ws://localhost:4348/"
        p (promise)
        client @(hato/websocket url {:on-message (fn [_ws msg _last?] (deliver p msg))})
        _ (hato/send! client "hello")]
    (is (= 1 @p))
    (hato/close! client)))

(deftest read-test
  (testing "read EVENT event type"
    (let [expected (mg/generate types/ResponseEvent)
          response-event-json (json/write-value-as-string expected)
          actual (-> response-event-json
                     (json/read-value json/keyword-keys-object-mapper)
                     relay/read-event)]
      (is (= actual expected))))

  (testing "read EOSE event type"
    (let [expected (mg/generate types/EoseEvent)
          response-event-json (json/write-value-as-string expected)
          actual (-> response-event-json
                     (json/read-value json/keyword-keys-object-mapper)
                     relay/read-event)]
      (is (= actual expected)))))

(deftest request-event-test
  (testing "Default request event"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:kinds [1]}]
             (relay/request-event subscription-id {})))))
  (testing "Cant override kinds"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:kinds [1]}]
             (relay/request-event subscription-id {:kinds [2]})))))
  (testing "Include limit filter"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:kinds [1] :limit 10}]
             (relay/request-event subscription-id {:limit 10}))))))
