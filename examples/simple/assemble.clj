(ns assemble-paper
  (:require [e-paper.storage :as ep])
  (:require [e-paper.hdf5 :as hdf5]))

; (prn "cwd:" (. System getProperty "user.dir"))

(def cwd "/Users/hinsen/projects/e-paper/examples/simple/")

(def paper (ep/create (str cwd "simple_paper.h5")))
(def clojure-jar (ep/store-jar paper "clojure"
                               (str cwd "code/clojure-1.2.0.jar")))
(def contrib-jar (ep/store-jar paper "clojure-contrib"
                               (str cwd "code/clojure-contrib-1.2.0.jar")))
(def script (ep/store-script paper "hello" (str cwd "code/hello.clj")
                             [clojure-jar contrib-jar]
                             "clojure.main" [:script-filename]))
(ep/close paper))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def paper (hdf5/open (str cwd "simple_paper.h5")))
(def script (hdf5/lookup paper "code/hello"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

