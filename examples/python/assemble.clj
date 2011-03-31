(ns python.assemble-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.hdf5 :as hdf5]))

(def cwd "/Users/hinsen/projects/e-paper/examples/python/")

(def paper (ep/create (str cwd "python_paper.h5")))
;; (def jython-jar (ep/store-jar paper "jython"
;;                               (str cwd "code/jython-2.5.2.jar")))
(def jython-jar (ep/store-code-reference paper "jython" "jython" "jython"))
(def script (ep/store-script paper "hello" (str cwd "code/hello.py")
                             "python" [jython-jar]))
(ep/close paper)
