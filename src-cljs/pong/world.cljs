(ns pong.world)

(defn change-world [state pred t]
  (let [entities (filter #(pred (last %)) (seq state))]
    (reduce #(t %1 (first %2) (last %2)) state entities)))

(defn change-entity [state pred t]
  (change-world state pred #(assoc %1 %2 (t %3))))
