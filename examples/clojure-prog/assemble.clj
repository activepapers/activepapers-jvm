(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def jars (ep/store-library-references paper "clojure"))

(ep/store-program paper "generate-input"
                  jars "clojure.main"
                  [{:type :text-file
                    :contents (slurp
                               (File. dir "code/generate_input.clj"))}])

(ep/run-program (ep/get-code paper "generate-input"))

(ep/store-program paper "calc-sine"
                  jars "clojure.main"
                  [{:type :text-file
                    :contents (slurp
                               (File. dir "code/calc_sine.clj"))}])

(ep/run-program (ep/get-code paper "calc-sine"))

(ep/close paper)
