(ns nostril.driven.ports
  (:refer-clojure :exclude [get]))

(defprotocol RelayGateway
  (connect! [this url])
  (put! [this relay-stream event])
  (close! [this relay-stream]))

(defprotocol EventStore
  (fetch-all [_this])
  (add-event! [_this event]))

(defprotocol RelayManager
  (add-relay! [this relay])
  (get-relay [this url])
  (remove-relay! [this url])
  (submit! [this url event]))


