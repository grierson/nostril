(ns nostril.core
  (:require
   [hashp.core]
   [manifold.stream :as s]
   [nostril.client :as client]
   [nostril.read :as read]
   [manifold.deferred :as d]
   [io.github.humbleui.ui :as ui]
   [io.github.humbleui.signal :as signal]))

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

(defonce *content (signal/signal "Relays"))

(ui/defcomp RelayContent []
  [ui/column
   [ui/row
    [ui/button {} [ui/label "Add Relay"]]
    [ui/rect {:paint {:fill 0xFFFEFEFE}}
     [ui/size {:height 30 :width 800}
      [ui/row
       ^{:stretch 1}
       [ui/align {:y :center}
        [ui/focusable {}
         [ui/on-key-focused {}
          [ui/with-cursor {:cursor :ibeam}
           [ui/text-input {}]]]]]]]]]
   [ui/gap {:height 10}]
   [ui/grid {:cols 1}
    (for [relay ["wss:this" "wss:that" "wss:other"]]
      [ui/padding
       {:padding 10}
       [ui/row
        [ui/align {:y :center}
         [ui/label relay]]
        [ui/button {} "Remove"]]])]])

(ui/defcomp AuthorsContent []
  [ui/column
   [ui/row
    [ui/button {} [ui/label "Add Author npub"]]
    [ui/rect {:paint {:fill 0xFFFEFEFE}}
     [ui/size {:height 30 :width 800}
      [ui/row
       ^{:stretch 1}
       [ui/align {:y :center}
        [ui/focusable {}
         [ui/on-key-focused {}
          [ui/with-cursor {:cursor :ibeam}
           [ui/text-input {}]]]]]]]]]
   [ui/gap {:height 10}]
   [ui/grid {:cols 1}
    (for [npubs ["npub1Jack" "npub1Snowden" "npub1Other"]]
      [ui/padding
       {:padding 10}
       [ui/row
        [ui/align {:y :center}
         [ui/label npubs]]
        [ui/button {} "Remove"]]])]])

(ui/defcomp EventsContent []
  [ui/grid {:cols 1}
   (for [events ["wss:this" "wss:that" "wss:other"]]
     [ui/padding
      {:padding 10}
      [ui/row
       [ui/align {:y :center}
        [ui/label events]]]])])

(ui/defcomp app []
  [ui/column {:width 150}
   [ui/row
    (list
     [ui/button
      {:on-click (fn [_]
                   (reset! *content "Relays"))}
      [ui/label "Relays"]]
     [ui/button
      {:on-click (fn [_]
                   (reset! *content "Authors"))}
      [ui/label "Authors"]]
     [ui/button
      {:on-click (fn [_]
                   (reset! *content "Events"))}
      [ui/label "Events"]])]
   [ui/padding
    {:padding 20}
    [(condp = @*content
       "Relays" RelayContent
       "Authors" AuthorsContent
       "Events" EventsContent)]]])

(defn -main [& args]
  (ui/start-app!
   (ui/window
    {:title    "Nostril"}
    #'app)))

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
;;
;; (reload)
