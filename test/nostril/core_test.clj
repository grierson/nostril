(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.core :as core]
   [hashp.core]
   [manifold.stream :as s]
   [jsonista.core :as json]))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate core/ResponseEvent)
          json-event (json/write-value-as-string expected)
          actual (core/read-event json-event)]
      (is (= expected actual)))))

(deftest read-stream-event
  (testing "reading from stream"
    (let [stream (s/stream)
          expected (mg/generate core/ResponseEvent)
          json-event (json/write-value-as-string expected)
          _ (s/put! stream json-event)
          actual-raw @(s/take! stream)]
      (is (= expected (core/read-event actual-raw))))))

(deftest read-multi-stream-event
  (testing "reading from stream"
    (let [relay1-stream (s/stream)
          relay2-stream (s/stream)
          main-stream (s/stream)
          _ (s/connect relay1-stream main-stream)
          _ (s/connect relay2-stream main-stream)
          expected1 (mg/generate core/ResponseEvent)
          expected2 (mg/generate core/ResponseEvent)
          json-event1 (json/write-value-as-string expected1)
          json-event2 (json/write-value-as-string expected2)
          _ (s/put! relay1-stream json-event1)
          _ (s/put! relay2-stream json-event2)
          actual-raw1 @(s/take! main-stream)
          actual-raw2 @(s/take! main-stream)]
      (is (= expected1 (core/read-event actual-raw1)))
      (is (= expected2 (core/read-event actual-raw2))))))
