(ns nostril.read-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [nostril.read :as read]
   [nostril.types :as types]))

(deftest read-test
  (testing "read EVENT event type"
    (let [expected (mg/generate types/ResponseEvent)
          response-event-json (json/write-value-as-string expected)
          actual (read/handle response-event-json)]
      (is (= actual expected)))))
