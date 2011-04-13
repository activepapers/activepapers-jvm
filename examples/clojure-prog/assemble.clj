(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.utility :as utility])
  (:require [clj-hdf5.core :as hdf5])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(hdf5/create-dataset paper "data/message" "Hello world")

(def clojure-jar (ep/store-code-reference paper
                           "clojure" "clojure" "clojure"))
(def clojure-contrib-jar (ep/store-code-reference paper
                           "clojure-contrib" "clojure" "clojure-contrib"))
(def clojure-hdf5-jar (ep/store-code-reference paper
                           "clojure-hdf5" "clojure" "clojure-hdf5"))

(def prog (ep/store-program paper "hello"
                            [clojure-jar clojure-contrib-jar clojure-hdf5-jar]
                            "clojure.main"
                            [{:type :text-file
                              :contents (slurp
                                         (File. dir "code/hello.clj"))}]))
(ep/close paper)
