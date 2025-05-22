(ns nostril.driving.ports)

(defprotocol DrivingPorts
  (for-add-relay! [this url])
  (for-close-relay! [this url])
  (for-send! [this url event])
  (for-get-events [this]))
