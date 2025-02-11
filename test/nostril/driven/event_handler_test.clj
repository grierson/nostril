(ns nostril.driven.event-handler-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.driven.event-handler :as event-handler]
   [nostril.driven.ports :as ports]
   [nostril.types :as types]))

(deftest AtomEventHandlerTest
  (testing "raise event to event store"
    (let [event-handler (event-handler/make-atom-event-handler)
          event (mg/generate types/ResponseEvent)
          _ (ports/raise! event-handler event)
          events (ports/fetch-all event-handler)
          first-event (first events)]
      (is (= event (:data first-event))))))
