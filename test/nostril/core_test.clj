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
