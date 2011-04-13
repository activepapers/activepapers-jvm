(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.utility :as utility])
  (:require [clj-hdf5.core :as hdf5])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def clojure-jar (ep/store-code-reference paper
                           "clojure" "clojure" "clojure"))
(def clojure-contrib-jar (ep/store-code-reference paper
                           "clojure-contrib" "clojure" "clojure-contrib"))
(def clojure-hdf5-jar (ep/store-code-reference paper
                           "clojure-hdf5" "clojure" "clojure-hdf5"))
(def jars [clojure-jar clojure-contrib-jar clojure-hdf5-jar])

(comment
  (hdf5/create-dataset paper "data/time" (vec (range 0. 10. 0.1)))
  (hdf5/create-dataset paper "data/frequency" 0.2))

(ep/store-program paper "generate-input"
                  jars "clojure.main"
                  [{:type :text-file
                    :contents (slurp
                               (File. dir "code/generate_input.clj"))}])

(ep/run-program (ep/get-program paper "generate-input"))

(ep/store-program paper "calc-sine"
                  jars "clojure.main"
                  [{:type :text-file
                    :contents (slurp
                               (File. dir "code/calc_sine.clj"))}])

(ep/run-program (ep/get-program paper "calc-sine"))

(ep/close paper)
