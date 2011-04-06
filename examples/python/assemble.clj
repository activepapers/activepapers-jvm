(ns python.assemble-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.jhdf5 :as hdf5])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/python/"))

(def paper (ep/create (File. dir "python_paper.h5")))
(def jython-jar (ep/store-code-reference paper "jython" "jython" "jython"))
(def script (ep/store-script paper "hello" (File. dir "code/hello.py")
                             "python" [jython-jar]))
(ep/close paper)
