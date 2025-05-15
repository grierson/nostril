(ns nostril.driven.event-handler-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as ports]
   [nostril.types :as types]))

(deftest AtomEventHandlerTest
  (testing "add event to event store"
    (let [event-store (event-handler/make-atom-event-store)
          event (mg/generate types/ResponseEvent)
          _ (ports/add-event! event-store event)
          events (ports/fetch-all event-store)
          first-event (first events)]
      (is (= event first-event)))))
