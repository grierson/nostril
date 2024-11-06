(ns nostril.util-test
  (:require [clojure.test :refer [deftest is testing]]
            [nostril.util :as util]))

(deftest parse-address
  (is (= "sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q"
         (util/parse-address "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m"))))

(deftest npub->hex-test
  (testing "convert jack npub to hex"
    (is (= "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2"
           (-> "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m"
               (util/address->number)
               (util/num32->hex-string))))))
