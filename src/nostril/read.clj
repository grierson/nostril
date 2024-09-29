(ns nostril.read
  (:require
   [nostril.types :as types]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]))

(defn process [event-json]
  (let [[type :as event] (json/read-value event-json json/keyword-keys-object-mapper)]
    (case type
      "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
      event)))
