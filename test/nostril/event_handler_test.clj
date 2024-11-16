(ns nostril.event-handler-test
  (:require
   [clojure.test :refer [deftest is testing]]
   [malli.generator :as mg]
   [nostril.types :as types]
   [nostril.event-handler :as event-handler]))

(deftest AtomEventHandler
  (testing "save"
    (let [handler (event-handler/make-atom-event-handler)
          [_ _ body :as event] (mg/generate types/ResponseEvent)
          event-id (:id body)
          _ (event-handler/save handler event)
          actual (event-handler/fetch-by-id handler event-id)]
      (is (= body actual))))

  (testing "fetch-by-id"
    (let [handler (event-handler/make-atom-event-handler)
          [_ _ body-1 :as event-1] (mg/generate types/ResponseEvent)
          [_ _ body-2 :as event-2] (mg/generate types/ResponseEvent)
          event-1-id (:id body-1)
          event-2-id (:id body-2)
          _ (event-handler/save handler event-1)
          _ (event-handler/save handler event-2)
          actual-1 (event-handler/fetch-by-id handler event-1-id)
          actual-2 (event-handler/fetch-by-id handler event-2-id)]
      (is (= body-1 actual-1))
      (is (= body-2 actual-2)))))

