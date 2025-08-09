(ns nostril.driving.humbleui
  (:require
   [io.github.humbleui.ui :as ui]
   [nostril.core :as nostril]
   [nostril.driven.event-store :as event-store]
   [nostril.driven.relay :as relay]
   [nostril.driving.ports :as driving-ports]
   [nostril.util :as util]
   [tick.core :as t]
   [nostril.dummy :as dummy]))

(defn unix-timestamp->str [timestamp]
  (let [duration (t/new-duration timestamp :seconds)
        instant (t/instant duration)
        zdt (t/zoned-date-time instant)
        formatter (t/formatter "yyyy-MM-dd HH:mm:ss")]
    (t/format formatter zdt)))

(def ^:dynamic *editing* false)

(defonce *user-interface (atom nil))
(defonce *application
  (nostril/make-application
   {:event-store (event-store/make-atom-event-store dummy/events)}))

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

(defonce *ui-state
  (ui/signal
   {:events []
    :new-relay {:text "wss://relay.damus.io"}}))

(def *new-relay-input (cursor *ui-state :new-relay))
(def *events (cursor *ui-state :events))

(defn relay-input []
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/size {:height 66}
    [ui/shadow-inset {:dy -2, :blur 1, :color 0x08000000}
     [ui/align {:y :center}
      [ui/focusable {}
       [ui/on-key-focused {:keymap {}}
        [ui/with-cursor {:cursor :ibeam}
         [ui/text-input {:*state *new-relay-input}]]]]]]]])

(defn draw-event [[_type _subid {:keys [content created_at]}]]
  [ui/rect {:paint {:fill 0xFFFEFEFE}}
   [ui/column
    [ui/gap {:height 10}]
    [ui/label content]
    [ui/gap {:height 10}]
    [ui/label (unix-timestamp->str created_at)]
    [ui/gap {:height 10}]]])

(comment (user/reload))

(defn main-view [application]
  [ui/hsplit
   {:width 250}
   [ui/column
    [ui/label {:font-size 50} "Nostril"]
    [ui/text-input {}]
    (relay-input)
    [ui/button
     {:on-click (fn [_]
                  (driving-ports/for-adding-relay! application (get-in @*ui-state [:new-relay :text]))
                  (println "added relay"))} [ui/label "Add Relay"]]
    [ui/button
     {:on-click (fn [_]
                  (driving-ports/for-sending-event! application
                                                    (get-in @*ui-state [:new-relay :text])
                                                    (relay/request-event {:since (- (util/now) 3600)
                                                                          :until (util/now)
                                                                          :limit 10}))
                  (println "fetch"))} [ui/label "Fetch events"]]
    [ui/button
     {:on-click (fn [_]
                  (let [events (driving-ports/for-getting-events *application)]
                    (swap! *ui-state update :events into events)
                    (println "update")))}
     [ui/label "Update events"]]
    [ui/gap {:height 20}]
    [ui/label {:font-size 30} "Relays"]
    [ui/gap {:height 20}]
    [ui/column
     (map (fn [relay] [ui/label relay]) (keys @(:relays application)))]]
   [ui/column
    [ui/label {:font-size 50} "Events"]
    [ui/vscroll
     [ui/column (map draw-event @*events)]]]])

(reset! *user-interface (fn [] (main-view *application)))

(defn -main []
  (ui/start-app! (ui/window {:title "Humble 🐝 UI"} *user-interface)))

(comment
  (-main)
  (user/reload))
