(ns hello.run-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.hdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/hello/")

(def paper (ep/open (str cwd "hello_paper.h5")))

(def script (ep/get-program paper "run-hello"))

(ep/run-program script)

(ep/close paper)
