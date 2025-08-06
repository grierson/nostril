(ns nostril.core-test
  (:require
   [clojure.core.async :as async]
   [clojure.test :refer [deftest is]]
   [jsonista.core :as json]
   [malli.core :as m]
   [malli.generator :as mg]
   [malli.transform :as mt]
   [nostril.core :as nostril]
   [nostril.driven.event-store :as event-store]
   [nostril.driven.relay :as relay]
   [nostril.driving.ports :as driving-ports]
   [nostril.types :as types]
   [org.httpkit.server :as http-kit]
   [awaitility-clj.core :refer [wait-for]]
   [prestancedesign.get-port :refer [get-port]]))

(defn create-ws-server
  "Creates an in-memory WebSocket server with a channel for message handling."
  [event]
  (let [messages (async/chan 10)
        handler (fn [request]
                  (http-kit/as-channel
                   request
                   {:on-receive (fn [channel data]
                                  (async/put! messages (json/write-value-as-string data))
                                  (http-kit/send! channel (json/write-value-as-string event)))
                    :on-close (fn [_channel status]
                                (println "WebSocket closed with status:" status))}))
        port (get-port)
        server (http-kit/run-server handler {:port port})]
    {:server server
     :messages messages
     :port port}))

(deftest application-test
  (let [event (mg/generate types/ResponseEvent)
        req-event (relay/request-event)
        {:keys [messages port]} (create-ws-server event)
        url (str "ws://localhost:" port)
        event-store (event-store/make-atom-event-store)
        application (nostril/make-application {:event-store event-store})
        _ (driving-ports/for-add-relay! application url)
        _ (driving-ports/for-send!
           application
           url
           req-event)
        message (async/<!! messages)
        _ (wait-for {:at-most [1 :seconds]}
                    (fn [] (>= 1 (count (event-store/fetch-all event-store)))))
        stored-events (event-store/fetch-all event-store)]
    (is (= req-event
           (m/decode types/RequestEvent
                     (json/read-value (json/read-value message) json/keyword-keys-object-mapper)
                     mt/string-transformer)))
    (is (= event (first stored-events)))))
