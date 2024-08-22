(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.core :as core]
   [hashp.core]
   [malli.core :as m]
   [malli.transform :as mt]
   [jsonista.core :as json]))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [expected (mg/generate core/ResponseEvent)
          [_ _ content] expected
          _ #p (type (java.time.Instant/now))
          _ #p (:created_at content)
          _ #p (type (:created_at content))
          encoded-event (m/encode core/ResponseEvent expected mt/string-transformer)
          [_ _ content2] encoded-event
          _ #p (:created_at content2)
          actual (core/read-event (json/write-value-as-string encoded-event))]
      (is (= expected actual)))))
