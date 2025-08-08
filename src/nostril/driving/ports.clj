(ns nostril.driving.ports)

(defprotocol DrivingPorts
  (for-getting-relays [_this])
  (for-getting-events [_this])
  (for-add-relay! [this url])
  (for-send! [this url event]))
