(defproject e-paper-runtime "0.0.1-SNAPSHOT"
  :description "Runtime library for Executable Paper Grand Challenge submission"
  :author      "Konrad Hinsen <research@khinsen.fastmail.net>"
  :aot [e-paper-runtime.data]
  :dependencies [[clojure "1.2.0"]
                 [clojure-contrib "1.2.0"]]
  :dev-dependencies [[swank-clojure "1.3.0-SNAPSHOT"]])
