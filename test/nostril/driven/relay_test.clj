(ns nostril.driven.relay-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [hashp.core]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.driven.relay :as relay]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as ports]
   [nostril.types :as types]))

(defn make-inmemory-relay-manager []
  (let [relay-stream (s/stream)
        event-handler (event-handler/make-atom-event-handler)
        relay-gateway (relay/->InMemoryRelayGateway relay-stream)
        relay-manager (relay/make-atom-hashmap-relay-manager event-handler relay-gateway)]
    relay-manager))

(deftest consume-test
  (testing "Raise event when Nostr event received"
    (let [relay-url "ws://nostr.relay"
          {:keys [event-handler] :as relay-manager} (make-inmemory-relay-manager)
          _ (ports/add-relay! relay-manager relay-url)
          [_event-type subscription-id :as event] (mg/generate types/ResponseEvent)
          _ (ports/submit! relay-manager relay-url event)
          events (ports/fetch-all event-handler)
          first-event (first events)
          [_event-type event-subscription-id] (:data first-event)]
      (is (= subscription-id event-subscription-id)))))

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
