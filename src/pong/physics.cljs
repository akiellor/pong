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

