(ns pong.core
  (:require
    [figwheel.client :as fw]
    [goog.events :as events]
    [pong.physics :as p]
    [pong.render :as r]))

(enable-console-print!)

(defn tap [value]
  (println value)
  value)

(defn score [state player]
  (let [current (get-in state [:game :scores player])]
    (-> state
      (assoc-in [:game :scores player] (inc current))
      (assoc-in [:ball :position] [0.5 0.5]))))

(defn score-right [state]
  (score state :right))

(defn score-left [state]
  (score state :left))

(defn flip-direction-y [object]
  (assoc-in object [:direction 1] (- (get-in object [:direction 1]))))

(defn flip-direction-x [object]
  (assoc-in object [:direction 0] (- (get-in object [:direction 0]))))

(defn reflect-ball [ball boundary direction]
  (let [[[ox oy] [odx ody]] (p/rect->cordinates ball)
        [[bx by] [bdx bdy]] (p/rect->cordinates boundary)]
    (case direction
      :up (-> ball
              (assoc-in [:position 1] (- by (get-in ball [:rect :height])))
              flip-direction-y)
      :down (-> ball
                (assoc-in [:position 1] bdy)
                flip-direction-y)
      :left (-> ball
                (assoc-in [:position 0] ox)
                flip-direction-x)
      :right (-> ball
                 (assoc-in [:position 0] bdx)
                 flip-direction-x))))


(defn reflect-ball-right [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :right)))

(defn reflect-ball-left [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :left)))

(defn reflect-ball-up [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :up)))

(defn reflect-ball-down [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :down)))

(defn left-up [state] (assoc-in state [:left :direction 1] -0.01))
(defn left-down [state] (assoc-in state [:left :direction 1] 0.01))
(defn left-stop [state] (assoc-in state [:left :direction 1] 0))
(defn right-up [state] (assoc-in state [:right :direction 1] -0.01))
(defn right-down [state] (assoc-in state [:right :direction 1] 0.01))
(defn right-stop [state] (assoc-in state [:right :direction 1] 0))

(def state (atom {:game {:scores {:left 0 :right 0}}
                  :left-text {:text (fn [state] (get-in state [:game :scores :left]))
                              :position [0.30 0.20]}
                  :left {:rect {:height 0.05 :width 0.01}
                         :position [0.05 0.5]
                         :on-collide reflect-ball-right
                         :keyboard {87 {:press left-up :release left-stop}
                                    83 {:press left-down :release left-stop}}
                         :direction [0 0]
                         :velocity 0.3}
                  :score-left {:rect {:height 0.96 :width 0.05}
                               :position [0 0.02]
                               :on-collide score-left}
                  :right-text {:text (fn [state] (get-in state [:game :scores :right]))
                               :position [0.70 0.20]}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.94 0.5]
                          :on-collide reflect-ball-left
                          :keyboard {38 {:press right-up :release right-stop}
                                     40 {:press right-down :release right-stop}}
                          :direction [0 0]
                          :velocity 0.3}
                  :score-right {:rect {:height 0.96 :width 0.05}
                                :position [0.95 0.02]
                                :on-collide score-right}
                  :lower-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.98]
                                   :on-collide reflect-ball-up}
                  :upper-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.01]
                                   :on-collide reflect-ball-down}
                  :ball {:rect {:height 0.01 :width 0.01}
                         :position [0.5 0.5]
                         :direction [0.01 0.01]
                         :velocity 0.2}}))

(defn physics [state]
  (-> state
      p/movement
      p/collisions))

(defn start-physics! []
  (js/setInterval (fn [] (swap! state physics)) 10))

(defn loop-render! [canvas]
  (.requestAnimationFrame js/window (fn [] (loop-render! canvas)))
  (r/render canvas @state))

(defn new-player-position [state player code keycodes]
  (let [current-position (get-in state [player :position 1])
        direction (get keycodes code)]
    (assoc-in state [player :position 1]
           (case direction
             :up (- current-position 0.05)
             :down (+ current-position 0.05)
             current-position))))

(defn input-handler [definition event keycode]
  (or (get-in definition [keycode event]) identity))

(defn keyboard? [value]
  (contains? value :keyboard))

(defn keyboard [state event keycode]
  (let [input-definitions (map :keyboard (filter keyboard? (vals state)))]
    (reduce #((input-handler %2 event keycode) %1) state input-definitions)))

(defn start-keyboard! []
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

(defn main []
  (let [canvas (.getContext (.getElementById js/document "app") "2d")]
    (set! (. canvas -height) 500)
    (set! (. canvas -width) 500)
    (loop-render! canvas)
    (start-physics!)
    (start-keyboard!)))

(fw/start {:on-jsload main})
