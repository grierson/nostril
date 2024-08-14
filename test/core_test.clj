(ns core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [core :as core]))

(deftest basic-test
  (testing "basic example"
    (is (= 2 (core/foo 2)))))
