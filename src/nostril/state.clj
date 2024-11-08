(ns nostril.state
  (:require [manifold.stream :as s]))

(defn add-relay [state relay-url relay-stream]
  (swap! state update :relays merge {relay-url relay-stream})
  (s/connect relay-stream (:source @state)))

(defn make-state []
  {:relays {}
   :source (s/stream)})

(comment
  {:relays {"wss://damus.io" "websocket connection"
            "wss://other" "websocket connection"}
   :stream "all relay websockets in one stream"})
