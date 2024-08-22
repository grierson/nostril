(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [nostril.core :as core]
   [hashp.core]))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate core/ResponseEvent)
          actual (core/read-event (json/write-value-as-string expected))]
      (is (= expected actual)))))
