(ns nostril.core
  (:require
   [nostril.read :as read]
   [hashp.core]
   [aleph.http :as http]
   [jsonista.core :as json]
   [manifold.stream :as s]))

(def relays (atom {}))

(defn fetch-request [subscription-id]
  ["REQ" subscription-id {:kinds [1] :limit 10}])

(defn close-request [subscription-id]
  ["CLOSE" subscription-id])

(defn subscribe [relays relay-configs]
  (reduce
   (fn [state {:keys [url subscription-id]}]
     (assoc state url {:stream @(http/websocket-client url)
                       :subscription-id subscription-id}))
   relays
   relay-configs))

(defn submit [relay-stream event]
  (s/put! relay-stream (json/write-value-as-string event)))

(defn unsubscribe [relays relay-url]
  (let [relay-config (get relays relay-url)
        result (submit
                (:stream relay-config)
                (close-request (:subscription-id relay-config)))]
    (when result (dissoc relays relay-url))))

(defn fetch [{:keys [stream subscription-id]}]
  (submit stream (fetch-request subscription-id)))

(defn append [events relay-stream]
  (let [event @(s/take! relay-stream)]
    (conj events (read/process event))))

(comment
  (swap! relays subscribe [{:url "wss://relay.damus.io"
                            :subscription-id "nostril-subid-damus"}
                           {:url "wss://purplepag.es"
                            :subscription-id "nostril-subid-purple"}])
  (fetch (get @relays "wss://purplepag.es"))
  (read/process @(s/take! (:stream (get @relays "wss://purplepag.es")))))
