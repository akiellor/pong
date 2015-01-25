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
