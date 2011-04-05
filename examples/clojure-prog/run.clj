(ns clojure-prog.run
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/clojure-prog/")

(def paper (ep/open (str cwd "clojure_paper.h5")))

(def prog (ep/get-program paper "hello"))

(ep/run-program prog)

(ep/close paper)
