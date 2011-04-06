(ns clojure-prog.run
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/open (File. dir "clojure_paper.h5")))

(def prog (ep/get-program paper "hello"))

(ep/run-program prog)

(ep/close paper)
