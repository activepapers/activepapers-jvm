(ns clojure-script.assemble
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-script/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def jars (ep/store-library-references paper "clojure"))

(ep/store-script paper "generate-input"
                 (File. dir "code/generate_input.clj")
                 "Clojure" jars)
(ep/run-script (ep/get-code paper "generate-input"))

(ep/store-script paper "calc-sine"
                 (File. dir "code/calc_sine.clj")
                 "Clojure" jars)
(ep/run-script (ep/get-code paper "calc-sine"))

(ep/close paper)
