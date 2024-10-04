(ns nostril.util)

(defn now-ms []
  (System/currentTimeMillis))

(defn now []
  (quot (now-ms) 1000))
