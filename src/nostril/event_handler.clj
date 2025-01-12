(ns nostril.event-handler)

(defprotocol EventHandler
  (fetch-all [_this])
  (raise [_this event]))

(defrecord AtomEventHandler [events]
  EventHandler
  (fetch-all [_this] @events)
  (raise [_this event]
    (swap! events conj event)))

(defn make-atom-event-handler []
  (->AtomEventHandler (atom [])))

(comment
  (def event-handler (make-atom-event-handler))
  (raise event-handler [1 2 {:id 123 :name "foo"}])
  (fetch-all event-handler))
