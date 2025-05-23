(ns nostril.driven.relay-manager-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hashp.core]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.driven.ports :as ports]
   [nostril.driven.relay :as relay]
   [nostril.types :as types]))

(defn make-inmemory-relay-manager [stream]
  (let [relay-gateway (relay/make-inmemory-relay-gateway stream)]
    (relay/make-atom-hashmap-relay-manager relay-gateway)))

(deftest submit-test
  (testing "Submits REQ as json"
    (let [stream (s/stream)
          url "ws://nostr.relay"
          relay-manager (make-inmemory-relay-manager stream)
          _ (ports/connect! relay-manager url)
          event (mg/generate types/RequestEvent)
          _  (ports/subscribe! relay-manager url event)]
      (is (= (json/write-value-as-string event)
             @(s/take! stream))))))

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
