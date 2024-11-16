(ns nostril.event-handler)

(defprotocol EventHandler
  (fetch-all [_this])
  (fetch-by-id [_this id])
  (save [_this event]))

(defrecord AtomEventHandler [events]
  EventHandler
  (fetch-all [_this] @events)
  (fetch-by-id [_this id] (get @events id))
  (save [_this [_ _ body]]
    (swap! events assoc (:id body) body)))

(defn make-atom-event-handler []
  (->AtomEventHandler (atom {})))

(comment
  (def foo (make-atom-event-handler))
  (save foo [1 2 {:id 123 :name "foo"}])
  (fetch-all foo)
  (fetch-by-id foo 123))
