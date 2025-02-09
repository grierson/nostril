(ns nostril.event-handler)

(defn nostr-event->event
  [[event-type :as event]]
  (when (contains? #{"EVENT" "EOSE"} event-type)
    {:id (random-uuid)
     :type :event-received
     :data-content-type event-type
     :data event}))

(defprotocol EventHandler
  (fetch-all [_this])
  (raise [_this event]))

(defrecord AtomEventHandler [events]
  EventHandler
  (fetch-all [_this] @events)
  (raise [_this event]
    (let [domain-event (nostr-event->event event)]
      (swap! events conj domain-event))))

(defn make-atom-event-handler []
  (->AtomEventHandler (atom [])))

(comment
  (def event-handler (make-atom-event-handler))
  (raise event-handler [1 2 {:id 123 :name "foo"}])
  (fetch-all event-handler))
