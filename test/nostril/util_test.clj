(ns nostril.util-test
  (:require
   [clojure.test :refer [deftest is testing]]))

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

(defn hexify [n]
  (num32->hex-string n))

(defn find-separator-char [address]
  (let [separator-pos (.lastIndexOf address "1")]
    (if (neg? separator-pos)
      (throw (Exception. "bech32: no separator character (1)"))
      separator-pos)))

(defn parse-address
  [address]
  (let [address (.toLowerCase address)
        hrp-end (find-separator-char address)
        data-and-cksum (subs address (inc hrp-end))
        data (apply str (drop-last 6 data-and-cksum))]
    data))

(def CHARSET "qpzry9x8gf2tvdw0s3jn54khce6mua7l")

(defn to-n [char] (.indexOf CHARSET (str char)))

(defn address->number [address]
  (let [data (parse-address address)
        data-bits (* 5 (count data))
        byte-over (rem data-bits 8)
        byte-over-correction (reduce * (repeat byte-over 2))
        values (map to-n data)
        acc (reduce (fn [n value]
                      (+ value (* n 32)))
                    0N values)]
    (/ acc byte-over-correction)))

(deftest npub->hex-test
  (testing "convert jack npub to hex"
    (is (= "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2"
           (-> "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m"
               (address->number)
               (hexify))))))
