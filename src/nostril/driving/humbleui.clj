(ns nostril.driving.humbleui
  (:require [io.github.humbleui.ui :as ui]))

(defonce *window (promise))

(defn relay-input []
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/shadow-inset {:dy -2, :blur 1, :color 0x08000000}
     [ui/align {:y :center}
      [ui/focusable {}
       [ui/on-key-focused {:keymap {}}
        [ui/with-cursor {:cursor :ibeam}
         [ui/text-input {}]]]]]]]])

(defn draw-event [{:keys [content]}]
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/label content]]])

(defn main-view []
  [ui/hsplit
   {:width 250}
   [ui/column
    [ui/label {:font-size 50} "Nostril"]
    [ui/text-input {}]
    (relay-input)
    [ui/button {} [ui/label "Add Relay"]]
    [ui/gap {:height 20}]
    [ui/label {:font-size 30} "Relays"]
    [ui/gap {:height 20}]
    [ui/label "ws://sample.com"]]
   [ui/column
    [ui/label {:font-size 50} "Events"]
    (map draw-event [{:content "hello"} {:content "world"} {:content "other"}])]])

(defonce *app (atom nil))
(reset! *app main-view)

(defn -main []
  (ui/start-app!
   (deliver *window (ui/window {:title "Humble 🐝 UI"} *app))) @*window)

(comment
  (-main)
  (user/reload))
