(ns pong.core
  (:require
    [figwheel.client :as fw]
    [pong.physics :as p]
    [pong.render :as r]
    [pong.keyboard :as k]))

(enable-console-print!)

(defn score [state player]
  (let [velocity (get-in state [:ball :velocity])]
    (-> state
      (update-in [:game :scores player] inc)
      (assoc-in [:ball :position] [0.5 0.5]))))

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

(def state (atom {:game {:scores {:left 0 :right 0}}
                  :left-text {:text (fn [state] (get-in state [:game :scores :left]))
                              :position [0.30 0.20]}
                  :left {:rect {:height 0.05 :width 0.01}
                         :position [0.05 0.5]
                         :on-collide #(p/reflect-right %1 :ball %2)
                         :keyboard {87 {:press left-up :release left-stop}
                                    83 {:press left-down :release left-stop}}
                         :velocity [0 0]}
                  :score-left {:rect {:height 0.96 :width 0.05}
                               :position [0 0.02]
                               :on-collide score-left}
                  :right-text {:text (fn [state] (get-in state [:game :scores :right]))
                               :position [0.70 0.20]}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.94 0.5]
                          :on-collide #(p/reflect-left %1 :ball %2)
                          :keyboard {38 {:press right-up :release right-stop}
                                     40 {:press right-down :release right-stop}}
                          :velocity [0 0]}
                  :score-right {:rect {:height 0.96 :width 0.05}
                                :position [0.95 0.02]
                                :on-collide score-right}
                  :lower-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.98]
                                   :on-collide #(p/reflect-up %1 :ball %2)}
                  :upper-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.01]
                                   :on-collide #(p/reflect-down %1 :ball %2)}
                  :ball {:rect {:height 0.01 :width 0.01}
                         :position [0.15 0.9]
                         :velocity [0.05 -0.05]}}))

(defn main []
  (let [canvas (.getContext (.getElementById js/document "app") "2d")]
    (set! (. canvas -height) 500)
    (set! (. canvas -width) 500)
    (r/start-render! state canvas)
    (p/start-physics! state)
    (k/start-keyboard! state)))

(fw/start {:on-jsload main})
