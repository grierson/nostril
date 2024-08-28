(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.core :as core]
   [hashp.core]
   [jsonista.core :as json]))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [expected #p (mg/generate core/ResponseEvent)
          actual #p (core/read-event (json/write-value-as-string expected))]
      (is (= expected actual)))))
