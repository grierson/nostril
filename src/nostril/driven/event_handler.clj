(ns nostril.driven.event-handler
  (:require
   [nostril.driven.ports :as ports]))

(defn nostr-event->event
  [[event-type _subscription-id _data :as event]]
  (when (contains? #{"EVENT" "EOSE"} event-type)
    {:id (random-uuid)
     :type :event-received
     :data-content-type event-type
     :data event}))

(defrecord AtomEventHandler [events]
  ports/EventHandler
  (fetch-all [_this] @events)
  (raise! [_this event]
    (let [domain-event (nostr-event->event event)]
      (swap! events conj domain-event))))

(defn make-atom-event-handler []
  (->AtomEventHandler (atom [])))

(comment
  (def event-handler (make-atom-event-handler))
  (ports/raise! event-handler [1 2 {:id 123 :name "foo"}])
  (ports/fetch-all event-handler))
