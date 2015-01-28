(ns pong.physics)

(def drag 0.01)

(defn move [moveable]
  (let [[px py] (:position moveable)
        [dx dy] (:velocity moveable)]
    (assoc-in moveable [:position]
              [(+ px (* dx drag)) (+ py (* dy drag))])))

(defn accelerate [moveable]
  (let [[ax ay] (or (:acceleration moveable) [0 0])
        [vx vy] (:velocity moveable)]
    (assoc-in moveable [:velocity] [(+ ax vx) (+ ay vy)])))

(defn moveable? [value]
  (contains? value :velocity))

(defn movement [state]
  (let [moveables (filter #(moveable? (last %)) (seq state))]
    (reduce #(assoc %1 (first %2) (move (accelerate (last %2)))) state moveables)))

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

(defn on-collides [state colls]
  (let [with-fn (filter #(:on-collide %) colls)]
    (reduce #((:on-collide %2) %1 %2) state with-fn)))

(defn flip-direction-y [object]
  (update-in object [:velocity 1] -))

(defn flip-direction-x [object]
  (update-in object [:velocity 0] -))

(defn bounce [entity coll]
  ((case (:surface coll)
        :horizontal flip-direction-y
        :vertical flip-direction-x
        :else identity) entity))

(defn abs [value] (max value (- value)))

(defn spin [entity coll]
  (let [py (or (get-in coll [:velocity 1]) 0)]
    (if (= py 0)
      entity
      (assoc entity :acceleration [0 (* 0.007 (/ py (abs py)))]))))

(defn reflects [entity colls]
  (reduce #(-> %1
               (bounce %2)
               (spin %2)) entity colls))

(defn entity-collisions [state name entity]
  (let [boundaries (filter #(contains? % :surface) (vals (dissoc state name)))
        colls (filter #(intersect? entity %) boundaries)]
    (-> state
        (update-in [name] #(reflects % colls))
        (on-collides colls))))

(defn collisions [state]
  (reduce #(entity-collisions %1 (first %2) (last %2)) state (filter #(:velocity (last %)) (seq state))))

(defn physics [state]
  (-> state
      movement
      collisions))

(defn start-physics! [state]
  (js/setInterval (fn [] (swap! state physics)) 10))
