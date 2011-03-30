(ns run-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.hdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/simple/")

(def paper (ep/open (str cwd "simple_paper.h5")))

(def script (ep/get-script paper "hello"))

(ep/run-script script))

(ep/close paper)
