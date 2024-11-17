(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.event-handler :as event-handler]
   [nostril.read :as read]
   [nostril.relay :as relay]
   [tick.core :as t]))

(defn callback [event-handler raw-event]
  (let [[type _subscription-id :as event] (read/handle raw-event)]
    (when (= type "EVENT")
      (event-handler/save event-handler event))))

(defn setup []
  (let [handler (event-handler/make-atom-event-handler)
        relays (atom {})
        stream (s/stream* {:permanent? true
                           :description "main stream for events"})
        _ (s/consume (partial callback handler) stream)]
    {:handler handler
     :relays relays
     :stream stream}))

(defn -main [& args]
  (let [{:keys [stream relays handler]} (setup)
        relay-url "wss://relay.damus.io"
        _ @(relay/add-relay stream relays relay-url)
        relay-stream (get @relays relay-url)
        _ @(relay/fetch-latest (t/clock) relay-stream)]
    true))

(comment
  (def system (setup))
  (def handler (:handler system))
  (def relays (:relays system))
  (def stream (:stream system))
  (def relay-url "wss://relay.damus.io")
  @(relay/add-relay stream relays relay-url)
  (def relay-stream (get @relays relay-url))
  (def submission @(relay/fetch-latest (t/clock) relay-stream))
  (event-handler/fetch-all handler)
  (count (event-handler/fetch-all handler)))
