(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.event-handler :as event-handler]
   [nostril.read :as read]
   [nostril.relay :as relay]
   [nostril.util :as util]))

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

(defn -main [& args]
  (let [handler (event-handler/make-atom-event-handler)
        relay-url "wss://relay.damus.io"
        relay-stream @(relay/connect relay-url)
        main-stream (s/stream* {:permanent? true
                                :description "main stream for events"})
        _ (s/connect relay-stream main-stream)
        event (request-event {:filters {:kinds [1]
                                        :since (- (util/now) 9999)
                                        :until (util/now)
                                        :limit 10}})
        _ @(relay/submit relay-stream event)]
    (s/consume (partial callback handler) main-stream)))

(comment
  (def damus-url "wss://relay.damus.io")
  (def damus-stream @(relay/connect damus-url))
  (def event-handler (event-handler/make-atom-event-handler))
  (def jacks-events (request-event {:filters {:kinds [1]
                                              :authors [jack-hex64]
                                              :limit 10}}))
  (def latest-events (request-event {:filters (merge {:kinds [1]
                                                      :limit 10}
                                                     (util/window 30))}))
  (def submit (relay/submit damus-stream latest-events))
  (def consumer (s/consume (partial callback event-handler) damus-stream))
  (count (event-handler/fetch-all event-handler))
  (map :content (vals (event-handler/fetch-all event-handler))))
