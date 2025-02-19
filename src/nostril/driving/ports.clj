(ns nostril.driving.ports)

(defprotocol DrivingPorts
  (get-events [this]))
