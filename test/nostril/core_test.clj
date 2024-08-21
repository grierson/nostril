(ns nostril.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [nostril.core :as core]
            [malli.generator :as mg]
            [jsonista.core :as json]
            [hashp.core]))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate core/ResponseEvent)
          actual (core/read-event (json/write-value-as-string expected))]
      (is (= expected actual)))))

(core/read-event (json/write-value-as-string (mg/generate core/ResponseEvent)))
