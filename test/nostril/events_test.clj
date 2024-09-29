(ns nostril.events-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [nostril.events :as events]
   [malli.generator :as mg]))

(deftest request-event-test
  (testing "REQ event"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {}]
             (events/request-event subscription-id)))))
  (testing "REQ event with filters"
    (let [subscription-id (mg/generate :string)]
      (is (= ["REQ" subscription-id {:limit 10}]
             (events/request-event subscription-id {:limit 10}))))))
