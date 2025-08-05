(ns nostril.driven.hato-test
  (:require
   [clojure.core.async :as async :refer [>! chan go]]
   [clojure.test :refer [deftest is]]
   [hashp.core]
   [jsonista.core :as json]
   [nostril.driven.relay :as relay :refer [request-event]]
   [org.httpkit.server :as http-kit]
   [prestancedesign.get-port :refer [get-port]]))

(defn create-ws-server
  "Creates an in-memory WebSocket server with a channel for message handling."
  []
  (let [messages (chan)
        handler (fn [request]
                  (http-kit/as-channel request
                                       {:on-receive (fn [_channel data]
                                                      (go (>! messages (json/write-value-as-string data))))
                                        :on-close (fn [_channel status]
                                                    (println "WebSocket closed with status:" status))}))
        port (get-port)
        server (http-kit/run-server handler {:port port})]
    {:server server
     :messages messages
     :port port}))

(deftest make-connection!-test
  (let [{:keys [messages port]} (create-ws-server)
        url (str "ws://localhost:" port)
        {:keys [in-channel]} (relay/make-connection! {:ws-type :hato :url url})
        event (request-event)]
    (async/put! in-channel event)
    (is (= (json/write-value-as-string event)
           (json/read-value (async/<!! messages))))))
