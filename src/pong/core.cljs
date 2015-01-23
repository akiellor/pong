(ns pong.core
  (:require
    [figwheel.client :as fw]
    [goog.events :as events]))

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

(defn rect->cordinates [rect]
  (let [[x y] (:position rect)]
    [[x y] [(+ x (get-in rect [:rect :width])) (+ y (get-in rect [:rect :height]))]]))

(defn intersect? [recta rectb]
  (let [[[ax ay] [adx ady]] (rect->cordinates recta)
        [[bx by] [bdx bdy]] (rect->cordinates rectb)]
    (and
      (or (<= ax bx adx bdx)
          (<= bx ax bdx adx)
          (<= ax bx bdx adx)
          (<= bx ax adx bdx))
      (or (<= ay by ady bdy)
          (<= by ay bdy ady)
          (<= ay by bdy ady)
          (<= by ay ady bdy)))))

(defn move [moveable]
  (let [[px py] (:position moveable)
        [dx dy] (:direction moveable)
        velocity (:velocity moveable)]
    (assoc-in moveable [:position]
              [(+ px (* velocity dx)) (+ py (* velocity dy))])))

(defn flip-direction-y [object]
  (assoc-in object [:direction 1] (- (get-in object [:direction 1]))))

(defn flip-direction-x [object]
  (assoc-in object [:direction 0] (- (get-in object [:direction 0]))))

(defn reflect-ball [ball boundary direction]
  (let [[[ox oy] [odx ody]] (rect->cordinates ball)
        [[bx by] [bdx bdy]] (rect->cordinates boundary)]
    (if (intersect? ball boundary)
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
                   flip-direction-x))
      ball)))


(defn reflect-ball-right [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :right)))

(defn reflect-ball-left [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :left)))

(defn reflect-ball-up [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :up)))

(defn reflect-ball-down [state boundary]
  (assoc-in state [:ball] (reflect-ball (:ball state) boundary :down)))

(def state (atom {:game {:scores {:left 0 :right 0}}
                  :left {:rect {:height 0.05 :width 0.01}
                         :position [0.05 0.5]
                         :on-collide reflect-ball-right}
                  :score-left {:rect {:height 0.96 :width 0.05}
                               :position [0 0.02]
                               :on-collide score-left}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.94 0.5]
                          :on-collide reflect-ball-left}
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
                         :direction [0.01 0]
                         :velocity 0.1}}))

(defn fill-style [canvas style]
  (set! (. canvas -fillStyle) style))

(defn fill-rect [canvas x y dx dy]
  (.fillRect canvas x y dx dy))

(defn percentage->coord [percentage height width]
  (let [[px py] percentage]
    [(* width px) (* height py)]))

(defn render-rect [canvas rect position]
  (let [cheight (.-height canvas)
        cwidth (.-width canvas)
        height (* cheight (:height rect))
        width (* cwidth (:width rect))
        [x y] (percentage->coord position cheight cwidth)]
    (fill-style canvas "white")
    (fill-rect canvas x y width height)))

(defn rect? [value] (and (contains? value :rect) (contains? value :position)))

(defn render-rects [canvas state]
  (let [rects (filter rect? (vals state))]
    (doall (map #(render-rect canvas (:rect %) (:position %)) rects))))

(defn render [canvas state]
  (let [height (.-height canvas)
        width (.-width canvas)]
    (fill-style canvas "black")
    (fill-rect canvas 0 0 width height)
    (render-rects canvas state)))

(defn collisions [object boundaries]
  (filter #(intersect? object %) boundaries))

(defn physics [state]
  (let [ball (:ball state)
        boundaries (filter #(contains? % :on-collide) (vals state))
        moved-ball (move ball)
        new-state (assoc state :ball moved-ball)
        colls (collisions moved-ball boundaries)]
    (reduce #((:on-collide %2) %1 %2) new-state colls)))

(defn start-physics! []
  (js/setInterval (fn [] (swap! state physics)) 10))

(defn loop-render! [canvas]
  (js/setInterval (fn [] (render canvas @state)) 100))

(defn new-player-position [state player code keycodes]
  (let [current-position (get-in state [player :position 1])
        direction (get keycodes code)]
    (assoc-in state [player :position 1]
           (case direction
             :up (- current-position 0.05)
             :down (+ current-position 0.05)
             current-position))))

(defn start-player! [player keycodes]
  (events/listen
    js/document goog.events.EventType.KEYDOWN
    (fn [evt]
      (let [code (-> evt .-keyCode)]
        (swap! state new-player-position player code keycodes)))))

(defn main []
  (let [canvas (.getContext (.getElementById js/document "app") "2d")]
    (set! (. canvas -height) 500)
    (set! (. canvas -width) 500)
    (loop-render! canvas)
    (start-physics!)
    (start-player! :left {87 :up 83 :down})
    (start-player! :right {38 :up 40 :down})))

(fw/start {:on-jsload main})
