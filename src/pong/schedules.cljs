(ns pong.schedules)

(defn seconds [value]
  value)

(defn schedule [state definition]
  (let [[name entity] definition
        in (get-in entity [:schedule :in])
        callback (get-in entity [:schedule :do])]
    (if (= in 0)
      (callback state)
      (update-in state [name :schedule :in] dec))))

(defn schedules [state]
  (let [schedules (filter #(:schedule (last %)) (seq state))]
    (reduce schedule state schedules)))

(defn start-schedules! [state]
  (js/setInterval (fn [] (swap! state schedules)) 1000))
