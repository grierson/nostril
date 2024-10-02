(ns nostril.util)

(defn get-now-ms []
  (System/currentTimeMillis))

(defn get-now []
  (quot (get-now-ms) 1000))
