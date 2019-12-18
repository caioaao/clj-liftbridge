(defproject clj-liftbridge "0.1.0-SNAPSHOT"
  :description "liftbridge client library"
  :url "https://github.com/caioaao/clj-liftbridge"
  :scm "https://github.com/caioaao/clj-liftbridge"
  :min-lein-version "2.0.0"
  :manifest {"GIT_COMMIT"   ~(System/getenv "GIT_COMMIT")
             "BUILD_NUMBER" ~(System/getenv "BUILD_NUMBER")}
  :license {:name "EPL-2.0 OR GPL-2.0-or-later WITH Classpath-exception-2.0"
            :url "https://www.eclipse.org/legal/epl-2.0/"}
  :source-paths ["src/clojure"]
  :java-source-paths ["src/java"]
  :dependencies [[org.clojure/clojure "1.10.0"]
                 [com.google.protobuf/protobuf-java "3.11.0"]]
  :profiles {:dev {:source-paths ["src/clojure" "dev"]
                   :repl-options {:init-ns user}
                   :dependencies [[hashp "0.1.0"]
                                  [orchestra "2018.12.06-2"]
                                  [org.clojure/tools.namespace "0.3.1"]]}})
