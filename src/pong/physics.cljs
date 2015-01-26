(ns pong.physics)

(defn move [moveable]
  (let [[px py] (:position moveable)
        [dx dy] (:direction moveable)
        velocity (:velocity moveable)]
    (assoc-in moveable [:position]
              [(+ px (* velocity dx)) (+ py (* velocity dy))])))

(defn moveable? [value]
  (and (contains? value :direction) (contains? value :velocity)))

(defn movement [state]
  (let [moveables (filter #(moveable? (last %)) (seq state))]
    (reduce #(assoc %1 (first %2) (move (last %2))) state moveables)))

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

(defn collisions [state]
  (let [ball (:ball state)
        boundaries (filter #(contains? % :on-collide) (vals state))
        colls (filter #(intersect? ball %) boundaries)]
    (reduce #((:on-collide %2) %1 %2) state colls)))

(defn physics [state]
  (-> state
      movement
      collisions))

(defn flip-direction-y [object]
  (update-in object [:direction 1] -))

(defn flip-direction-x [object]
  (update-in object [:direction 0] -))

(defn reflect [object boundary direction]
  (let [[[ox oy] [odx ody]] (rect->cordinates object)
        [[bx by] [bdx bdy]] (rect->cordinates boundary)]
    (case direction
      :up (-> object
              (assoc-in [:position 1] (- by (get-in object [:rect :height])))
              flip-direction-y)
      :down (-> object
                (assoc-in [:position 1] bdy)
                flip-direction-y)
      :left (-> object
                (assoc-in [:position 0] ox)
                flip-direction-x)
      :right (-> object
                 (assoc-in [:position 0] bdx)
                 flip-direction-x))))


(defn reflect-right [state entity boundary]
  (update-in state [entity] #(reflect % boundary :right)))

(defn reflect-left [state entity boundary]
  (update-in state [entity] #(reflect % boundary :left)))

(defn reflect-up [state entity boundary]
  (update-in state [entity] #(reflect % boundary :up)))

(defn reflect-down [state entity boundary]
  (update-in state [entity] #(reflect % boundary :down)))

(defn start-physics! [state]
  (js/setInterval (fn [] (swap! state physics)) 10))
