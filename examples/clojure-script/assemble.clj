(ns clojure-script.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/clojure-script/")

(def paper (ep/create (str cwd "clojure_paper.h5")))
(def clojure-jar (ep/store-code-reference paper
                           "clojure" "clojure" "clojure"))
(def contrib-jar (ep/store-code-reference paper
                           "clojure-contrib" "clojure" "clojure-contrib"))
(def scripting-jar (ep/store-code-reference paper
                           "clojure-scripting" "clojure" "clojure-scripting"))
(def script (ep/store-script paper "hello"
                             (str cwd "code/hello.clj")
                             "Clojure"
                              [clojure-jar contrib-jar scripting-jar]))
(ep/close paper)
