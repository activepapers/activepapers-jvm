(ns clojure-scripting.example
  (:require [active-papers.storage :as ep])
  (:use [active-papers.authoring :only (script)])
  (:use [active-papers.execution :only (run-calclet)])
  (:import java.io.File))

(def paper (ep/create (File. "clojure_example.h5")))

(def jars (ep/store-library-references paper "clojure"))

; The real calclets are run immediately.
(run-calclet
 (script paper "generate-input" "Clojure" jars
" (ns generate-input
    (:require [active-paper-clojure-runtime.data :as data]))
  (data/create-data \"time\" (vec (range 0. 10. 0.1)))
  (data/create-data \"frequency\" 0.2)
"))

(run-calclet
 (script paper "calc-sine" "Clojure" jars
" (ns calc-sine
    (:require [active-paper-clojure-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
  (let [time      (hdf5/read (data/get-data \"time\"))
        frequency (hdf5/read (data/get-data \"frequency\"))
        sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
    (data/create-data \"sine\" sine))
"))

; REPL and swank-server are for calclet development
(script paper "repl" "Clojure" jars
"(ns repl
    (:require clojure.main)
    (:require [active-paper-clojure-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
 (clojure.main/repl)
")

(script paper "swank-server" "Clojure" jars
"(ns calclet
    (:require [active-paper-clojure-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5])
    (:require [swank.swank]))
 (swank.swank/start-repl)
")

(ep/close paper)
