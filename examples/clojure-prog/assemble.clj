(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.authoring :as auth])
  (:require [e-paper.execution :as execution])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def jars (ep/store-library-references paper "clojure"))

; REPL and swank-server are for calclet development
(auth/clojure-script paper jars
  (ns repl
    (:require clojure.main)
    (:require [e-paper-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
  (clojure.main/repl))

(auth/clojure-script paper jars
  (ns swank-server)
  (ns user
    (:require [e-paper-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5])
    (:require [swank.swank]))
  (swank.swank/start-repl))

; The real calclets are run immediately.
(execution/run-calclet
 (auth/clojure-script paper jars
   (ns generate-input
     (:require [e-paper-runtime.data :as data]))
   (data/create-data "time" (vec (range 0. 10. 0.1)))
   (data/create-data "frequency" 0.2)))

(execution/run-calclet
 (auth/clojure-script paper jars
   (ns calc-sine
     (:require [e-paper-runtime.data :as data])
     (:require [clj-hdf5.core :as hdf5]))
   (let [time      (hdf5/read (data/get-data "time"))
         frequency (hdf5/read (data/get-data "frequency"))
         sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
     (data/create-data "sine" sine))))

(ep/close paper)
