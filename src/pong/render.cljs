(ns pong.render)

(defn fill-style [canvas style]
  (set! (. canvas -fillStyle) style))

(defn fill-rect [canvas x y dx dy]
  (.fillRect canvas x y dx dy))

(defn fill-text [canvas text x y]
  (set! (. canvas -font) "48px Monospace")
  (set! (. canvas -textAlign) "center")
  (.fillText canvas text x y))

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

(defn text? [value] (and (contains? value :text) (contains? value :position)))

(defn render-text [canvas text position]
  (let [cheight (.-height canvas)
        cwidth (.-width canvas)
        [x y] (percentage->coord position cheight cwidth)]
    (fill-style canvas "white")
    (fill-text canvas text x y)))

(defn render-texts [canvas state]
  (let [texts (filter text? (vals state))]
    (doall (map #(render-text canvas ((:text %) state) (:position %)) texts))))

(defn render [canvas state]
  (let [height (.-height canvas)
        width (.-width canvas)]
    (fill-style canvas "black")
    (fill-rect canvas 0 0 width height)
    (render-rects canvas state)
    (render-texts canvas state)))

(defn start-render! [state canvas]
  (.requestAnimationFrame js/window (fn [] (start-render! state canvas)))
  (render canvas @state))
