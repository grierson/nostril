(ns nostril.read
  (:require
   [nostril.types :as types]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]))

(defmulti process
  (fn [x]
    (let [[type & _] (json/read-value x)]
      type)))

(defmethod process "EVENT" [json-event]
  (let [event (json/read-value json-event json/keyword-keys-object-mapper)]
    (m/decode types/ResponseEvent event mt/string-transformer)))

(defmethod process "EOSE" [json-event]
  (println "eose")
  (println json-event))

(defmethod process :default [json-event]
  (println "default")
  (println json-event))

(comment
  {:relays {:name {:stream "steam" :subscription-id "whatever"}
            :name2 {:stream "steam2" :subscription-id "whatever"}}
   :events {"<event-id>" "event"
            "<event-id2>" "event2"}})
