(defproject klipse "7.6.1"
  :description "Embeddable multi-language WEB REPL"
  :resource-paths ["scripts" "src" "resources" "target"]
  :clean-targets ^{:protect false} ["resources/public/dev/js"]
  :min-lein-version "2.8.1"
  :middleware [lein-tools-deps.plugin/resolve-dependencies-with-deps-edn]
  :lein-tools-deps/config {:config-files [:install :user :project]}
  :plugins [[lein-cljsbuild "1.1.5"]
            [lein-hiera "0.9.5"]
            [lein-tools-deps "0.4.1"]]
  :hiera {:path "deps-graph.png"
          :vertical true
          :show-external false
          :cluster-depth 2
          :trim-ns-prefix true}
  :cljsbuild {:builds {:app {
                             :source-paths ["src/klipse/run/app"]
                             :compiler {
                                        :output-to "resources/public/dev/js/klipse.js"
                                        :output-dir "resources/public/dev/js"
                                        :pretty-print true
                                        :optimize-constants true
                                        :static-fns true
                                        ;:elide-asserts true
                                        :closure-defines {klipse.core/version
                                                          ~(->> (slurp "project.clj")
                                                             (re-seq #"\".*\"")
                                                             (first))}
                                        :optimizations :simple
                                        :verbose false}}}})
