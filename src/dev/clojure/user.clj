(ns user
  (:require [hashp.core]
            [clojure.main]
            [clojure.tools.namespace.repl :refer [refresh refresh-all]]
            [orchestra.spec.test :as st]))

(set! *warn-on-reflection* true)

(defn -main [& args]
  (st/instrument)
  (apply clojure.main/main args))
