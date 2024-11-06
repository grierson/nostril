(ns nostril.core-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [jsonista.core :as json]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.core :as core]
   [nostril.types :as types]))

(deftest callback-test
  (testing "appends new event to store"
    (let [stream (s/stream)
          events (atom [])
          event (mg/generate types/ResponseEvent)
          raw-event (json/write-value-as-string event)
          _ (core/callback stream events raw-event)]
      (is (= [event] @events))))

  (testing "sends CLOSE event when EOSE event received"
    (let [stream (s/stream)
          [_ subscription-id :as eose-event] (mg/generate types/EoseEvent)
          raw-event (json/write-value-as-string eose-event)
          _ (core/callback stream nil raw-event)
          raw-close-event (s/take! stream)
          close-event (json/read-value @raw-close-event)]
      (is (= ["CLOSE" subscription-id] close-event)))))
