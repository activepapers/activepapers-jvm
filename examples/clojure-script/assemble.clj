(ns clojure-script.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-script/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))
(def clojure-jar (ep/store-code-reference paper
                           "clojure" "clojure" "clojure"))
(def contrib-jar (ep/store-code-reference paper
                           "clojure-contrib" "clojure" "clojure-contrib"))
(def scripting-jar (ep/store-code-reference paper
                           "clojure-scripting" "clojure" "clojure-scripting"))
(def script (ep/store-script paper "hello"
                             (File. dir "code/hello.clj")
                             "Clojure"
                              [clojure-jar contrib-jar scripting-jar]))
(ep/close paper)
