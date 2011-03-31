(ns hello.assemble-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.hdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/hello/")

(def paper (ep/create (str cwd "hello_paper.h5")))
(def hello-jar (ep/store-jar paper "hello"
                               (str cwd "code/hello.jar")))
(def script (ep/store-program paper "run-hello" nil
                              [hello-jar]
                              "HelloWorld" []))
(ep/close paper))
