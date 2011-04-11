(ns clojure-script.run-paper
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-script/"))

(def paper (ep/open (File. dir "clojure_paper.h5")))

(def script (ep/get-program paper "hello"))

(ep/run-script script)

(ep/close paper)
