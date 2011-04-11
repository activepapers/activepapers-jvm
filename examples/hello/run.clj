(ns hello.run-paper
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/hello/"))

(def paper (ep/open (File. dir "hello_paper.h5")))

(def program (ep/get-program paper "run-hello"))

(ep/run-program program)

(ep/close paper)
