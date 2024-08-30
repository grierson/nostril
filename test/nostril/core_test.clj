(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.core :as core]
   [nostril.read :as read]
   [nostril.types :as types]
   [hashp.core]
   [manifold.stream :as s]
   [jsonista.core :as json]))

(deftest process-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate types/ResponseEvent)
          json-event (json/write-value-as-string expected)
          actual (read/process json-event)]
      (is (= expected actual)))))

(deftest fetch-request-test
  (testing "writing to stream"
    (let [relay-url "wss://sample.com"
          subscription-id (mg/generate :string)
          stream (s/stream)
          state {relay-url {:stream stream
                            :subscription-id subscription-id}}
          request (core/fetch-request subscription-id)
          _ (core/send-event state relay-url request)
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
