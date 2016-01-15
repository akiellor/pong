(ns pong.server
  (:require [org.httpkit.server :refer [run-server]]
            [ring.middleware.resource :refer [wrap-resource]]
            [ring.middleware.file-info :refer [wrap-file-info]])
  (:gen-class :main true))

(defn request-logging [app]
  (fn [request]
    (let [params (select-keys request [:request-method :uri :content-type])
          message (clojure.string/join " " (map #(apply format "%s=%s" %) params))]
      (println message)
      (app request))))

(defn handler [request]
  {:status 302
   :headers {"Location" "/index.html"}})

(def app
  (-> handler
      (wrap-resource "public")
      (wrap-file-info)
      (request-logging)))

(defn -main [port]
  (run-server #'app {:port (java.lang.Integer/parseInt port)}))
