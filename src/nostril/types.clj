(ns nostril.types
  (:require
   [clojure.string :as string]
   [malli.generator :as mg]))

(def hex-32 [:string {:min 64 :max 64}])
(def hex-64 [:string {:min 128 :max 128}])
(def Relay-url [:? uri?])
(def Timestamp [:int {:min 0 :max 9999999999999}])
(def Subscription-id [:and
                      string?
                      [:fn #(<= (count %) 64)]
                      [:fn #(not (string/blank? %))]])

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

(def RequestEvent
  [:catn
   [:type [:= "REQ"]]
   [:subscription-id Subscription-id]
   [:event Event]])

(def ResponseEvent
  [:catn
   [:type [:= "EVENT"]]
   [:subscription-id Subscription-id]
   [:event Event]])

(def EoseEvent
  [:catn
   [:type [:= "EOSE"]]
   [:subscription-id Subscription-id]])

(comment
  (mg/generate RequestEvent)
  (mg/generate ResponseEvent))
