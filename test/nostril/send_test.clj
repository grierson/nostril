(ns nostril.send-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.send :as send]))

(deftest fetch-test
  (testing "submits fetch event"
    (let [stream (s/stream)
          subscription-id (mg/generate :string)
          relay-config {:stream stream
                        :subscription-id subscription-id}
          _ (send/fetch relay-config)
          stream-event @(s/take! stream)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (send/request-event subscription-id)
             event)))))

(deftest close-test
  (testing "submits unsubscribe event"
    (let [stream (s/stream)
          subscription-id (mg/generate :string)
          relay-config {:stream stream
                        :subscription-id subscription-id}
          result (send/close relay-config)
          stream-event @(s/take! stream)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (send/close-event subscription-id) event))
      (is (true? @result)))))

(deftest request-event-test
  (testing "REQ event"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {}]
             (send/request-event subscription-id)))))
  (testing "REQ event with filters"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:limit 10}]
             (send/request-event subscription-id {:limit 10}))))))
