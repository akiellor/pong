(ns pong.world)

(defn change [state pred reducer]
  (let [entities (filter #(pred (last %)) (seq state))]
    (reduce #(assoc %1 (first %2) (reducer (last %2))) state entities)))
