(ns nostril.driven.ports)

(defprotocol EventStore
  (fetch-all [_this])
  (add-event! [_this event]))

(defprotocol RelayManager
  (connect! [this url])
  (disconnect! [this url])
  (subscribe! [this url event]))

(defprotocol RelayGateway
  (make-connection! [this url])
  (close-connection! [_this])
  (send! [this event]))
