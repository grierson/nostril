(ns nostril.driving.ports)

(defprotocol DrivingPorts
  (for-getting-relays [_this])
  (for-adding-event! [_this event])
  (for-getting-events [_this])
  (for-adding-relay! [this url])
  (for-sending-event! [this url event]))
