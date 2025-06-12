(ns nostril.driven.event-handler)

(defprotocol EventStore
  (fetch-all [_this])
  (add-event! [_this event]))

(defrecord AtomEventStore [events]
  EventStore
  (fetch-all [_this] @events)
  (add-event! [_this event] (swap! events conj event)))

(defn make-atom-event-store []
  (->AtomEventStore (atom [])))
