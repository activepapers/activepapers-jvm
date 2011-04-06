(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5])
  (:require [e-paper.utility :as utility])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))
(def clojure-jar (ep/store-code-reference paper
                           "clojure" "clojure" "clojure"))
(def prog (ep/store-program paper "hello"
                            [clojure-jar]
                            "clojure.main"
                            [{:type :text-file
                              :contents (slurp
                                         (File. dir "code/hello.clj"))}]))
(ep/close paper)
