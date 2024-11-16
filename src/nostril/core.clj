(ns nostril.core
  (:require
   [hashp.core]
   [manifold.deferred :as d]
   [manifold.stream :as s]
   [nostril.relay :as relay]
   [nostril.humbleui :as ui]
   [nostril.read :as read]
   [nostril.event-handler :as event-handler]))

;; Should be 64 hex for authors filter
(def jack "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m")
(def jack-hex64 "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2")

(defn request-event
  [{:keys [filters subscription-id]
    :or {subscription-id (str (random-uuid))
         filters {:kind [1]}}}]
  ["REQ" subscription-id filters])

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn callback [event-handler raw-event]
  (let [[type _subscription-id :as event] (read/handle raw-event)]
    (when (= type "EVENT")
      (event-handler/save event-handler event))))

(defn -main [& args] (ui/make-app))

(comment
  (def damus-url "wss://relay.damus.io")
  (def damus-stream @(relay/connect damus-url))
  (def event-handler (event-handler/make-atom-event-handler))
  (def submit
    (-> (relay/submit
         damus-stream
         (request-event {:filters {:kinds [1]
                                   :authors [jack-hex64]
                                   :limit 10}}))
        (d/catch #(str "something unexpected submitting: " (.getMessage %)))))
  (def consumer
    (-> (s/consume (partial callback event-handler) damus-stream)
        (d/catch #(str "something unexpected consuming: " (.getMessage %)))))
  (count (event-handler/fetch-all event-handler)))
