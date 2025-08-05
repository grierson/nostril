(ns nostril.driven.hato-test
  (:require [clojure.test :refer [deftest is]]
            [clojure.core.async :as async :refer [chan go >!]]
            [jsonista.core :as json]
            [prestancedesign.get-port :refer [get-port]]
            [hato.websocket :as hato]
            [hashp.core]
            [org.httpkit.server :as http-kit]))

(defn create-ws-server
  "Creates an in-memory WebSocket server with a channel for message handling."
  []
  (let [messages (chan)
        handler (fn [request]
                  (http-kit/as-channel request
                                       {:on-receive (fn [channel data]
                                                      (go (>! messages (json/write-value-as-string data))))
                                        :on-close (fn [channel status]
                                                    (println "WebSocket closed with status:" status))}))
        port (get-port)
        server (http-kit/run-server handler {:port port})]
    {:server server
     :messages messages
     :port port}))

(create-ws-server)

(deftest test-websocket-message
  (let [{:keys [messages port]} (create-ws-server)
        conn @(hato/websocket (str "ws://localhost:" port) {})]
    (hato/send! conn (json/write-value-as-string "hello"))
    (is (= "\"hello\"" (json/read-value (async/<!! messages))))))
