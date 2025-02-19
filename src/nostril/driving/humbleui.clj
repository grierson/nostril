(ns nostril.driving.humbleui
  (:require
   [hashp.core]
   [io.github.humbleui.signal :as signal]
   [io.github.humbleui.ui :as ui]
   [tick.core :as t]
   [nostril.driving.ports :as driving-ports]))

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

;; [ui/align {:y :center}
;;      [ui/vscroll
;;       [ui/align {:x :center}
;;        [ui/padding {:padding 20}
;;         [ui/grid {:cols (count header)
;;                   :rows (inc (count currencies))}
;;          (concat
;;            (for [[th i] (util/zip header (range))]
;;              [ui/clickable
;;               {:on-click (on-click i)}
;;               [ui/padding {:padding 10}
;;                [ui/reserve-width
;;                 {:probes [[ui/label (str th " ⏶")]
;;                           [ui/label (str th " ⏷")]]}
;;                 [ui/align {:x :left}
;;                  [ui/label {:font-weight :bold}
;;                   (str th
;;                     (case (when (= i sort-col)
;;                             sort-dir)
;;                       :asc  " ⏶"
;;                       :desc " ⏷"
;;                       nil   ""))]]]]])
;;            (for [row currencies
;;                  s   row]
;;              [ui/padding {:padding 10}
;;               [ui/label s]]))]]]]]

(ui/defcomp EventsContent []
  [ui/grid {:cols 1}
   (for [event []]
     (let [[_id body] event]
       [ui/rect {:paint [{:fill   "FFDB2C80"}
                         {:stroke "808080"}]}
        [ui/padding {:padding 10}
         [ui/column {:gap 10}
          [ui/label (:content body)]
          [ui/label {:paint {:fill 0xFFCC33FE}} (t/>> (t/epoch) (t/new-duration (:created_at body) :seconds))]
          [ui/label (:pubkey body)]]]]))])

(t/>> (t/epoch) (t/new-duration 1731881784 :seconds))

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

(defn make-app [system]
  (ui/start-app!
   (ui/window
    {:title    "Nostril"}
    #'app)))
