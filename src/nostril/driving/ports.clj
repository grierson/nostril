(ns nostril.driving.ports)

(defprotocol DrivingPorts
  (for-add-relay! [this url])
  (for-send! [this url event]))
