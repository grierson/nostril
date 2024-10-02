(ns nostril.store
  (:require
   [nostril.read :as read]
   [nostril.client :as client]))

(defn subscribe [relays {:keys [url] :as relay-config}]
  (if (get relays url)
    relays
    (assoc relays url (client/create-connection relay-config))))

(defn append [events relay-stream]
  (let [event (client/pull relay-stream)
        [_ _ body :as event] (read/handle event)]
    (assoc events (:id body) event)))


