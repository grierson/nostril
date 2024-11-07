(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.read :as read]
   [manifold.deferred :as d]
   [io.github.humbleui.ui :as ui]
   [nostril.state :as state])
  (:import
   [io.github.humbleui.skija Color ColorSpace]
   [io.github.humbleui.jwm Window]
   [io.github.humbleui.jwm.skija LayerMetalSkija]))

;; Should be 64 hex for authors filter
(def jack "npub1sg6plzptd64u62a878hep2kev88swjh3tw00gjsfl8f237lmu63q0uf63m")
(def jack-hex64 "82341f882b6eabcd2ba7f1ef90aad961cf074af15b9ef44a09f9d2a8fbfbe6a2")

(def connections (atom {}))
(def events (atom []))

(defn request-event
  ([] (request-event {}))
  ([filters] ["REQ" (str (random-uuid)) filters]))

(defn close-event [subscription-id]
  ["CLOSE" subscription-id])

(defn callback [stream events raw-event]
  (let [[type subscription-id :as event] (read/handle raw-event)]
    (condp = type
      "EVENT" (swap! events conj event)
      "EOSE" (client/submit stream (close-event subscription-id)))))

(defn draw-event [[_type _id body]]
  (let [content (get body :content)]
    [ui/paragraph content]))

(def app
  "Main app definition."
  (fn []
    [ui/default-theme
     {}
     [ui/center
      [ui/grid {:cols 1
                :gap 8}
       (map draw-event @events)
       [ui/label "End of Events"]]]]))

;; reset current app state on eval of this ns
(reset! state/*app app)

(defn -main
  "Run once on app start, starting the humble app."
  [& args]
  (ui/start-app!
   (reset! state/*window
           (ui/window
            {:title    "Editor"
             :bg-color 0xFFFFFFFF}
            state/*app)))
  (state/redraw!))

(-main)

(comment
  (swap! connections client/connect "wss://relay.damus.io")
  (def damus-stream (get @connections "wss://relay.damus.io"))
  (def submit
    (-> (client/submit
         damus-stream
         (request-event {:kinds [1]
                         :limit 10
                         :authors [jack-hex64]}))
        (d/catch #(str "something unexpected submitting: " (.getMessage %)))))
  (def consumer
    (-> (s/consume (partial callback damus-stream events) damus-stream)
        (d/catch #(str "something unexpected consuming: " (.getMessage %)))))
  (count @events))

;; run (reset! state/*app app) to reload ui
