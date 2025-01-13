(ns nostril.types
  (:require
   [clojure.string :as string]
   [malli.generator :as mg]))

(def hex-32 [:string {:min 64 :max 64}])
(def hex-64 [:string {:min 128 :max 128}])
(def Relay-url [:? uri?])
(def Timestamp [:int {:min 0 :max 9999999999999}])

(def TagE [:catn
           [:type [:= "e"]]
           [:event-id hex-32]
           [:relay-url Relay-url]])

(def TagP [:catn
           [:type [:= "p"]]
           [:event-id hex-32]
           [:relay-url Relay-url]])

(def TagA [:catn
           [:type [:= "a"]]
           [:event-rn hex-32]
           [:relay-url Relay-url]])

(def Event
  [:map
   [:id hex-32]
   [:pubkey hex-32]
   [:created_at Timestamp]
   [:kind [:int {:min 0 :max 65535}]]
   [:tags [:sequential [:alt TagA TagE TagP]]]
   [:content :string]
   [:sig hex-64]])

(def ResponseEvent
  [:catn
   [:type [:= "EVENT"]]
   [:subscription-id [:and string? [:fn #(not (string/blank? %))]]]
   [:event Event]])

(def EoseEvent
  [:catn
   [:type [:= "EOSE"]]
   [:subscription-id [:and string? [:fn #(not (string/blank? %))]]]])

(comment
  (mg/generate ResponseEvent))
