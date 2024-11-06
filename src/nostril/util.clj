(ns nostril.util)

(defn now-ms []
  (System/currentTimeMillis))

(defn now []
  (quot (now-ms) 1000))

(defn num->bytes
  "Returns the byte-array representation of n.
  The array will have the specified length."
  [length n]
  (let [a (.toByteArray (biginteger n))
        l (count a)
        zeros (repeat (- length l) (byte 0))]
    (if (> l length)
      (byte-array (drop (- l length) (seq a)))
      (byte-array (concat zeros a)))))

(defn bytes->hex-string
  "Returns a string containing the hexadecimal
  representation of the byte-array. This is the
  inverse of hex-string->bytes."
  [byte-array]
  (let [byte-seq (for [i (range (alength byte-array))] (aget byte-array i))
        byte-strings (map #(apply str (take-last 2 (format "%02x" %))) byte-seq)]
    (apply str (apply concat byte-strings))))

(defn num32->hex-string [n]
  (->> n (num->bytes 32) bytes->hex-string))

(defn parse-address [address] (subs address 5 57))
(def CHARSET "qpzry9x8gf2tvdw0s3jn54khce6mua7l")
(defn char->index [char] (.indexOf CHARSET (str char)))

(defn address->number [address]
  (let [data (parse-address address)
        values (map char->index data)
        acc (reduce (fn [n value] (+ value (* n 32))) 0N values)]
    (/ acc 16)))
