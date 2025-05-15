(ns nostril.driven.relay-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hashp.core]
   [manifold.stream :as s]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [nostril.driven.relay :as relay]
   [nostril.driven.ports :as ports]
   [nostril.types :as types]))

(defn make-inmemory-relay-manager []
  (let [relay-gateway (relay/make-inmemory-relay-gateway)]
    (relay/make-atom-hashmap-relay-manager relay-gateway)))

(deftest consume-test
  (testing "Raise event when Nostr event received"
    (let [relay-url "ws://nostr.relay"
          relay-manager (make-inmemory-relay-manager)
          _ (ports/add-relay! relay-manager relay-url)
          relay-connection (ports/get-relay relay-manager relay-url)
          event (mg/generate types/ResponseEvent)
          _ (ports/get-relay relay-manager relay-url)
          _  (ports/submit! relay-manager relay-url event)]
      (is (= event
             (json/read-value
              @(s/take! (:stream relay-connection))
              json/keyword-keys-object-mapper))))))

(deftest read-test
  (testing "read EVENT event type"
    (let [expected (mg/generate types/ResponseEvent)
          response-event-json (json/write-value-as-string expected)
          foo (json/read-value response-event-json json/keyword-keys-object-mapper)
          actual (relay/read-event foo)]
      (is (= actual expected))))

  (testing "read EOSE event type"
    (let [expected (mg/generate types/EoseEvent)
          response-event-json (json/write-value-as-string expected)
          foo (json/read-value response-event-json json/keyword-keys-object-mapper)
          actual (relay/read-event foo)]
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
  (testing "Include limit request event"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:kinds [1] :limit 10}]
             (relay/request-event subscription-id {:limit 10}))))))
