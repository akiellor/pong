(ns pong.core
  (:require
    [pong.physics :as p]
    [pong.render :as r]
    [pong.keyboard :as k]
    [pong.schedules :as s]))

(enable-console-print!)

(defn vanilla-ball []
  {:rect {:height 0.01 :width 0.01}
   :bouncy true
   :acceleration [0 0]
   :velocity [0.5 0.5]
   :position [0.5 0.5]})

(defn start-game [state]
  (-> state
      (dissoc :countdown)
      (assoc :ball (vanilla-ball))))

(defn vanilla-countdown []
  {:text (fn [state] (get-in state [:countdown :schedule :in]))
   :size "100px"
   :position [0.5 0.5]
   :schedule {:in (s/seconds 3) :do start-game}})

(defn score [state player]
  (let [velocity (get-in state [:ball :velocity])]
    (-> state
      (update-in [:game :scores player] inc)
      (assoc :countdown (vanilla-countdown))
      (dissoc :ball))))

(defn score-right [state]
  (score state :right))

(defn score-left [state]
  (score state :left))

(def paddle-speed 0.7)
(def paddle-stop 0)

(defn left-up [state] (assoc-in state [:left :velocity 1] (- paddle-speed)))
(defn left-down [state] (assoc-in state [:left :velocity 1] paddle-speed))
(defn left-stop [state] (assoc-in state [:left :velocity 1] paddle-stop))
(defn right-up [state] (assoc-in state [:right :velocity 1] (- paddle-speed)))
(defn right-down [state] (assoc-in state [:right :velocity 1] paddle-speed))
(defn right-stop [state] (assoc-in state [:right :velocity 1] paddle-stop))

(def vanilla-game-state {
                  :tick 0
                  :game {:scores {:left 0 :right 0}}
                  :left-text {:text (fn [state] (get-in state [:game :scores :left]))
                              :position [0.30 0.20]}
                  :left {:rect {:height 0.15 :width 0.01}
                         :position [0.055 0.5]
                         :surface :vertical
                         :range-y [0.02 0.98]
                         :keyboard {87 {:press left-up :release left-stop}
                                    83 {:press left-down :release left-stop}}
                         :velocity [0 0]}
                  :score-left {:rect {:height 0.96 :width 0.05}
                               :position [0 0.02]
                               :surface :vertical
                               :on-collide score-left}
                  :right-text {:text (fn [state] (get-in state [:game :scores :right]))
                               :position [0.70 0.20]}
                  :right {:rect {:height 0.15 :width 0.01}
                          :position [0.935 0.5]
                          :surface :vertical
                          :range-y [0.02 0.98]
                          :keyboard {38 {:press right-up :release right-stop}
                                     40 {:press right-down :release right-stop}}
                          :velocity [0 0]}
                  :score-right {:rect {:height 0.96 :width 0.05}
                                :position [0.95 0.02]
                                :surface :vertical
                                :on-collide score-right}
                  :lower-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.98]
                                   :surface :horizontal}
                  :upper-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.01]
                                   :surface :horizontal}
                  :countdown (vanilla-countdown)})

(def state (atom vanilla-game-state))

(defn set-acceleration [x y]
  (swap! state #(assoc-in vanilla-game-state [:ball :acceleration] [x y])))

(defn inc-tick [state]
  (update-in state [:tick] inc))

(defn tick [state]
  (do
    (swap! state (fn [v] (-> v
                             inc-tick
                             s/schedules
                             p/physics)))))

(defn start-tick! [state]
  (js/setInterval #(tick state) 10))

(defn main []
  (let [canvas (.getContext (.getElementById js/document "app") "2d")]
    (set! (. canvas -height) 500)
    (set! (. canvas -width) 500)
    (k/start-keyboard! state)
    (r/start-render! state canvas)
    (start-tick! state)))

(main)
