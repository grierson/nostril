(ns nostril.driven.ports)

(defprotocol RelayGateway
  (connect! [this url])
  (close! [this relay-stream]))

(defprotocol EventHandler
  (fetch-all [_this])
  (raise [_this event]))

(defprotocol RelayManager
  (add-relay! [this relay])
  (get-relay [this url])
  (remove-relay! [this url])
  (submit! [this url event]))


