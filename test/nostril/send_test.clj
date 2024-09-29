(ns nostril.send-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.send :as send]))

(deftest submit-test
  (testing "submits event as json"
    (let [subscription-id (mg/generate :string)
          stream (s/stream)
          request (send/fetch-request subscription-id)
          _ (send/submit stream request)
          stream-event @(s/take! stream)
          map-event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (true? (string? stream-event)))
      (is (= request map-event)))))

(deftest fetch-test
  (testing "submits fetch event"
    (let [stream (s/stream)
          subscription-id (mg/generate :string)
          relay-config {:stream stream
                        :subscription-id subscription-id}
          _ (send/fetch relay-config)
          stream-event @(s/take! stream)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (send/fetch-request subscription-id) event)))))

(deftest unsubscribe-test
  (testing "submits unsubscribe event"
    (let [stream (s/stream)
          subscription-id (mg/generate :string)
          relay-config {:stream stream
                        :subscription-id subscription-id}
          _ (send/unsubscribe relay-config)
          stream-event @(s/take! stream)
          event (json/read-value stream-event json/keyword-keys-object-mapper)]
      (is (= (send/close-request subscription-id) event)))))
