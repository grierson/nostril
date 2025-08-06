(ns nostril.driven.event-store-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.driven.event-store :as event-store]
   [nostril.types :as types]))

(deftest AtomEventStoreTest
  (testing "add event to event store"
    (let [event-store (event-store/make-atom-event-store)
          event (mg/generate types/ResponseEvent)
          _ (event-store/add-event! event-store event)
          events (event-store/fetch-all event-store)
          first-event (first events)]
      (is (= event first-event)))))
