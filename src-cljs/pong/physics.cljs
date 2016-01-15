(ns pong.physics
  (:require [pong.world :as w]))

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
  (w/change-entity state moveable? #(-> % move accelerate)))

(defn rect->cordinates [rect]
  (let [[x y] (:position rect)]
    [[x y] [(+ x (get-in rect [:rect :width])) (+ y (get-in rect [:rect :height]))]]))

(defn intersect? [recta rectb]
  (let [[[ax ay] [adx ady]] (rect->cordinates recta)
        [[bx by] [bdx bdy]] (rect->cordinates rectb)]
    (and
      (or (< ax bx adx bdx)
          (< bx ax bdx adx)
          (< ax bx bdx adx)
          (< bx ax adx bdx))
      (or (< ay by ady bdy)
          (< by ay bdy ady)
          (< ay by bdy ady)
          (< by ay ady bdy)))))

(defn on-collide? [entity colls]
  (and (:on-collide entity) (some #{entity} colls)))

(defn on-collides [state colls]
  (w/change-world state #(on-collide? % colls) #((:on-collide %3) %1)))

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

(defn limited? [entity]
  (contains? entity :range-y))

(defn limit [state name entity]
  (let [[ry rdy] (:range-y entity)
        height (get-in entity [:rect :height])
        max-dy (- rdy height)
        [[_ y] [_ dy]] (rect->cordinates entity)]
    (assoc state name (cond
                        (> ry y) (assoc-in entity [:position 1] ry)
                        (< rdy dy) (assoc-in entity [:position 1] max-dy)
                        :else entity))))

(defn limits [state]
  (w/change-world state limited? limit))

(defn reflects [entity colls]
  (if (:bouncy entity)
    (reduce #(-> %1
                 (bounce %2)
                 (spin %2)) entity colls)
    entity))

(defn entity-collisions [state name entity]
  (let [boundaries (filter #(contains? % :surface) (vals (dissoc state name)))
        colls (filter #(intersect? entity %) boundaries)]
    (-> state
        (update-in [name] #(reflects % colls))
        (on-collides colls))))

(defn moving? [entity]
  (let [velocity (:velocity entity)]
    (and velocity (not (= velocity [0 0])))))

(defn collisions [state]
  (w/change-world state moving? entity-collisions))

(def physics (comp movement collisions limits))

(defn start-physics! [state]
  (js/setInterval (fn [] (swap! state physics)) 10))
