(ns nostril.store
  (:require
   [nostril.read :as read]
   [hashp.core]
   [aleph.http :as http]
   [manifold.stream :as s]))

(defn subscribe [relays relay-configs]
  (reduce
   (fn [state {:keys [url subscription-id]}]
     (if (get state url)
       state
       (assoc state url {:stream @(http/websocket-client url)
                         :subscription-id subscription-id})))
   relays
   relay-configs))

(defn append [events relay-stream]
  (let [event @(s/take! relay-stream)
        [_ _ body :as event] (read/process event)]
    (assoc events (:id body) event)))
