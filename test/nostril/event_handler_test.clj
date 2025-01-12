(ns nostril.event-handler-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.types :as types]
   [nostril.event-handler :as event-handler]))

(deftest AtomEventHandler
  (testing "raise event to event store"
    (let [handler (event-handler/make-atom-event-handler)
          event (mg/generate types/ResponseEvent)
          _ (event-handler/raise handler {:type :event-received
                                          :payload event})
          events (event-handler/fetch-all handler)
          first-event (first events)]
      (is (= {:type :event-received
              :payload event}
             first-event)))))

