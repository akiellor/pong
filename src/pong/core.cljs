(ns pong.core
  (:require
    [figwheel.client :as fw]
    [goog.events :as events]))

(enable-console-print!)

(def state (atom {:left {:rect {:height 0.05 :width 0.01}
                         :position [0 0.5]}
                  :right {:rect {:height 0.05 :width 0.01}
                          :position [0.99 0.5]}
                  :ball {:rect {:height 0.01 :width 0.01}
                         :position [0.5 0.5]}}))

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

(defn render [canvas state]
  (let [height 500
        width 500]
    (fill-style canvas "black")
    (fill-rect canvas 0 0 width height)
    (render-rect canvas (:rect (:ball state)) (:position (:ball state)))
    (render-rect canvas (:rect (:left state)) (:position (:left state)))
    (render-rect canvas (:rect (:right state)) (:position (:right state)))))

(defn new-ball-position [state]
  (let [ball (:ball state)
        [x y] (:position ball)
        [dx dy] (:direction ball)]
    state))

(defn start-ball! []
  (js/setInterval (fn [] (swap! state new-ball-position)) 100))

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
    (start-ball!)
    (start-player! :left {87 :up 83 :down})
    (start-player! :right {38 :up 40 :down})))

(fw/start {:on-jsload main})
