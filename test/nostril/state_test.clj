(ns nostril.state-test
  (:require
   [hashp.core]
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [manifold.stream :as s]
   [nostril.state :refer [make-state add-relay]]
   [nostril.types :as types]))

(deftest add-relay-test
  (testing "adding new relay"
    (let [state (atom (make-state))
          relay-url "relay-url"
          relay-stream (s/stream)
          event (mg/generate types/Event)
          _ (add-relay state relay-url relay-stream)
          _ (s/put! relay-stream event)
          source-event @(s/take! (:source @state))]
      (is (= event source-event)))))



