(ns clojure-example.build
  (:require [active-papers.storage :as ep])
  (:require [active-papers.authoring :as auth])
  (:require [active-papers.execution :as execution])
  (:import java.io.File))

(def paper (ep/create (File. "clojure_example.h5")))

(def jars (ep/store-library-references paper "clojure"))

; REPL and swank-server are for calclet development
(auth/clojure-script paper jars
  (ns repl
    (:require clojure.main)
    (:require [active-paper-clojure-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
  (clojure.main/repl))

(auth/clojure-script paper jars
  (ns swank-server)
  (ns user
    (:require [active-paper-clojure-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5])
    (:require [swank.swank]))
  (swank.swank/start-repl))

; Input parameters
(ep/create-data paper "frequency" 0.2)
(ep/create-data paper "time" (vec (range 0. 10. 0.1)))

; The real calclets are run immediately.
(execution/run-calclet
 (auth/clojure-script paper jars
   (ns calc-sine
     (:require [active-paper-clojure-runtime.data :as data])
     (:require [clj-hdf5.core :as hdf5]))
   (let [time      (hdf5/read (data/get-data "time"))
         frequency (hdf5/read (data/get-data "frequency"))
         sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
     (data/create-data "sine" sine))))

(ep/close paper)
