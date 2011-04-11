(ns python.run-paper
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/python/"))

(def paper (ep/open (File. dir "python_paper.h5")))

(def script (ep/get-program paper "hello"))

(ep/run-script script)

(ep/close paper)
