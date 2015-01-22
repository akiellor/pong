(ns pong.core
  (:require
    [figwheel.client :as fw]
    [goog.events :as events]))

(enable-console-print!)

(def state (atom {:left {:rect {:height 0.05 :width 0.01}
                         :position [0 0.5]
                         :reflect :right}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.99 0.5]
                          :reflect :left}
                  :lower-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.98]
                                   :reflect :up}
                  :upper-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.01]
                                   :reflect :down}
                  :ball {:rect {:height 0.01 :width 0.01}
                         :position [0.97 0.5]
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

(defn move [moveable]
  (let [[px py] (:position moveable)
        [dx dy] (:direction moveable)
        velocity (:velocity moveable)]
    (assoc-in moveable [:position]
              [(+ px (* velocity dx)) (+ py (* velocity dy))])))

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

(defn flip-direction-y [object]
  (assoc-in object [:direction 1] (- (get-in object [:direction 1]))))

(defn flip-direction-x [object]
  (assoc-in object [:direction 0] (- (get-in object [:direction 0]))))

(defn collision [object boundary]
  (let [[[ox oy] [odx ody]] (rect->cordinates object)
        [[bx by] [bdx bdy]] (rect->cordinates boundary)]
    (if (intersect? object boundary)
      (case (:reflect boundary)
        :up (-> object
                (assoc-in [:position 1] by)
                flip-direction-y)
        :down (-> object
                  (assoc-in [:position 1] bdy)
                  flip-direction-y)
        :left (-> object
                  (assoc-in [:position 0] ox)
                  flip-direction-x)
        :right (-> object
                   (assoc-in [:position 0] bdx)
                   flip-direction-x))
      object)))

(defn collisions [object boundaries]
  (reduce collision object boundaries))

(defn physics [state]
  (let [ball (:ball state)
        lower-boundary (:lower-boundary state)
        upper-boundary (:upper-boundary state)
        left (:left state)
        right (:right state)]
    (assoc-in state [:ball] (-> ball
                                move
                                (collisions [left right upper-boundary lower-boundary])))))

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
