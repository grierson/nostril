(ns nostril.read
  (:require
   [clojure.pprint :as pprint]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.transform :as mt]
   [nostril.types :as types]))

(defn handle [event-json]
  (let [_ (prn event-json)
        [type :as event] (json/read-value event-json json/keyword-keys-object-mapper)]
    (case type
      "EVENT" (m/decode types/ResponseEvent event mt/string-transformer)
      "NOTICE" (pprint/pprint event)
      "EOSE" (m/decode types/EoseEvent event mt/string-transformer)
      event)))
