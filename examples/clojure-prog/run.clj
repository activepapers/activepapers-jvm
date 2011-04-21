(ns clojure-prog.run
  (:require [e-paper.storage :as ep])
  (:require [e-paper.execution :as run])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/open (File. dir "clojure_paper.h5")))

(run/rebuild-from-primary-items paper (File. dir "clone.h5"))

(ep/close paper)

