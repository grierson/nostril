(ns nostril.humbleui
  (:require
   [hashp.core]
   [io.github.humbleui.ui :as ui]
   [io.github.humbleui.signal :as signal]))

(defonce *content (signal/signal "Events"))

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
   (for [events ["wss:this" "wss:that" "wss:foo"]]
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

(defn make-app []
  (ui/start-app!
   (ui/window
    {:title    "Nostril"}
    #'app)))
