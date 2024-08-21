(ns nostril.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [nostril.core :as core]
            [malli.generator :as mg]
            [tick.core :as t]
            [jsonista.core :as json]
            [hashp.core]))

(defn now-unix []
  (-> (t/now)
      (t/instant)
      (.getEpochSecond)))

(deftest read-event-test
  (testing "reading EVENT event"
    (let [event (mg/generate core/Event)
          response (json/write-value-as-string ["EVENT" "subid" event])
          [_type _subid res-event] (core/read-event response)]
      (is (= #p res-event event)))))
