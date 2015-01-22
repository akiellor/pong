(ns pong.core
  (:require
    [figwheel.client :as fw]
    [goog.events :as events]))

(enable-console-print!)

(def state (atom {:left {:rect {:height 0.05 :width 0.01}
                         :position [0 0.5]}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.99 0.5]}
                  :lower-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.98]
                                   :reflect :up}
                  :upper-boundary {:rect {:height 0.01 :width 1}
                                   :position [0 0.01]
                                   :reflect :down}
                  :ball {:rect {:height 0.01 :width 0.01}
                         :position [0.1 0.3]
                         :direction [0.01 -0.01]
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

(defn collision [object boundary]
  (let [orect (:rect object)
        [_ opy] (:position object)
        [_ ody] (:direction object)
        brect (:rect boundary)
        [_ by] (:position boundary)
        reflect (:reflect boundary)]
    (cond
      (and (>= opy by) (= reflect :up)) (-> object
                                            (assoc-in [:position 1] (+ by (- by opy)))
                                            (assoc-in [:direction 1] (- ody)))
      (and (<= opy by) (= reflect :down)) (-> object
                                              (assoc-in [:position 1] (+ by (- by opy)))
                                              (assoc-in [:direction 1] (- ody)))
 
      :else object)))

(defn collisions [object boundaries]
  (reduce collision object boundaries))

(defn physics [state]
  (let [ball (:ball state)
        lower-boundary (:lower-boundary state)
        upper-boundary (:upper-boundary state)]
    (assoc-in state [:ball] (-> ball
                                move
                                (collisions [lower-boundary upper-boundary])))))

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
