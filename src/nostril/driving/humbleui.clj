(ns nostril.driving.humbleui
  (:require
   [io.github.humbleui.ui :as ui]
   [nostril.core :as nostril]))

(def ^:dynamic *editing* false)

(defn cursor-in [*signal path]
  (let [*res (ui/signal (get-in @*signal path))]
    (add-watch *signal *res
               (fn [_ _ old new]
                 (let [old (get-in old path)
                       new (get-in new path)]
                   (when (not= old new)
                     (when-not *editing*
                       (binding [*editing* true]
                         (reset! *res new)))))))
    (add-watch *res ::source
               (fn [_ _ old new]
                 (when (not= old new)
                   (when-not *editing*
                     (binding [*editing* true]
                       (swap! *signal assoc-in path new))))))
    *res))

(defn cursor [*signal key]
  (cursor-in *signal [key]))

(def *state
  (ui/signal
   {:new-relay {:text "wss://relay.damus.io"}}))

(def *new-relay-input (cursor *state :new-relay))

(defn relay-input []
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/shadow-inset {:dy -2, :blur 1, :color 0x08000000}
     [ui/align {:y :center}
      [ui/focusable {}
       [ui/on-key-focused {:keymap {}}
        [ui/with-cursor {:cursor :ibeam}
         [ui/text-input {:*state *new-relay-input}]]]]]]]])

(defn draw-event [{:keys [content]}]
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/label content]]])

(defn main-view [{:keys [relays event-store]}]
  [ui/hsplit
   {:width 250}
   [ui/column
    [ui/label {:font-size 50} "Nostril"]
    [ui/text-input {}]
    (relay-input)
    [ui/button {:on-click (fn [_] (println "hello"))} [ui/label "Add Relay"]]
    [ui/gap {:height 20}]
    [ui/label {:font-size 30} "Relays"]
    [ui/gap {:height 20}]
    [ui/label "ws://sample.com"]]
   [ui/column
    [ui/label {:font-size 50} "Events"]
    (map draw-event [{:content "this"} {:content "world"} {:content "this"}])]])

(defonce *app (atom nil))
(defonce *state (atom (nostril/make-application)))
(reset! *app (fn [] (main-view @*state)))

(defn -main []
  (ui/start-app! (ui/window {:title "Humble 🐝 UI"} *app)))

(comment
  (-main)
  (user/reload))
