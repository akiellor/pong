(ns pong.keyboard
  (:require
    [goog.events :as events]))

(defn input-handler [definition event keycode]
  (or (get-in definition [keycode event]) identity))

(defn keyboard? [value]
  (contains? value :keyboard))

(defn keyboard [state event keycode]
  (let [input-definitions (map :keyboard (filter keyboard? (vals state)))]
    (reduce #((input-handler %2 event keycode) %1) state input-definitions)))

(defn start-keyboard! [state]
  (events/listen
    js/document goog.events.EventType.KEYDOWN
    (fn [evt]
      (let [keycode (-> evt .-keyCode)]
        (swap! state keyboard :press keycode))))
  (events/listen
    js/document goog.events.EventType.KEYUP
    (fn [evt]
      (let [keycode (-> evt .-keyCode)]
        (swap! state keyboard :release keycode)))))
