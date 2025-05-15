(ns nostril.driven.event-handler
  (:require [nostril.driven.ports :as ports]))

(defrecord AtomEventStore [events]
  ports/EventStore
  (fetch-all [_this] @events)
  (add-event! [_this event] (swap! events conj event)))

(defn make-atom-event-store []
  (->AtomEventStore (atom [])))
