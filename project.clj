(defproject clj-liftbridge "0.1.0-SNAPSHOT"
  :description "liftbridge client library"
  :url "https://github.com/caioaao/clj-liftbridge"
  :scm "https://github.com/caioaao/clj-liftbridge"
  :min-lein-version "2.0.0"
  :manifest {"GIT_COMMIT"   ~(System/getenv "GIT_COMMIT")
             "BUILD_NUMBER" ~(System/getenv "BUILD_NUMBER")}
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url  "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths ["src/main/clojure"]
  :java-source-paths ["src/main/java"
                      "src/auto/java"]
  :resource-paths ["resources/main"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.google.protobuf/protobuf-java "3.11.0"]
                 [io.grpc/grpc-protobuf "1.26.0"]
                 [io.grpc/grpc-stub "1.26.0"]]
  :profiles {:dev {:source-paths ["src/main/clojure" "src/dev/clojure"]
                   :resource-paths ["resources/dev"]
                   :repl-options {:init-ns user}
                   :dependencies [[hashp "0.1.0"]
                                  [orchestra "2018.12.06-2"]
                                  [org.clojure/tools.namespace "0.3.1"]
                                  [less-awful-ssl "1.0.4"]
                                  [io.grpc/grpc-netty "1.26.0"]]}})
