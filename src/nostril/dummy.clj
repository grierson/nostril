(ns nostril.dummy
  (:require
   [malli.generator :as mg]
   [nostril.types :as types]))

(def events [(mg/generate types/ResponseEvent) (mg/generate types/ResponseEvent) (mg/generate types/ResponseEvent)])
