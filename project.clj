(defproject pong "0.1.0-SNAPSHOT"
  :description "A simple pong game implemented in ClojureScript"

  :dependencies [[org.clojure/clojure "1.7.0"]
                 [org.clojure/clojurescript "1.7.170"]
                 [http-kit "2.1.18"]
                 [ring/ring-core "1.2.2"]]

  :plugins [[lein-cljsbuild "1.1.1"]
            [lein-figwheel "0.5.0-3"]]

  :source-paths ["src-clj"]

  :hooks [leiningen.cljsbuild]

  :clean-targets ^{:protect false} ["resources/public/js/compiled" "target"]

  :cljsbuild {:builds
              [{:id "dev"
                :source-paths ["src-cljs"]

                :figwheel true

                :compiler {:main pong.core
                           :asset-path "js/compiled/out"
                           :output-to "resources/public/js/compiled/pong.js"
                           :output-dir "resources/public/js/compiled/out"
                           :source-map-timestamp true}}
               ;; This next build is an compressed minified build for
               ;; production. You can build this with:
               ;; lein cljsbuild once min
               {:id "min"
                :source-paths ["src-cljs"]
                :compiler {:output-to "resources/public/js/compiled/pong.js"
                           :main pong.core
                           :optimizations :advanced
                           :pretty-print false}}]}
  :main pong.server
  :aot :all
  :java-target "1.7"
  :uberjar-name "pong-standalone.jar")
