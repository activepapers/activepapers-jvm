(ns python.run-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/python/")

(def paper (ep/open (str cwd "python_paper.h5")))

(def script (ep/get-program paper "hello"))

(ep/run-script script)

(ep/close paper)
