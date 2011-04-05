(ns clojure-script.run-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/clojure-script/")

(def paper (ep/open (str cwd "clojure_paper.h5")))

(def script (ep/get-program paper "hello"))

(ep/run-script script)

(ep/close paper)
