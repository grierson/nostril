(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [nostril.core :as core]
   [nostril.types :as types]))

(deftest callback-test
  (testing "appends new event to store"
    (let [events (atom [])
          event (mg/generate types/ResponseEvent)
          raw-event (json/write-value-as-string event)
          _ (core/callback events raw-event)]
      (is (= [event] @events)))))
