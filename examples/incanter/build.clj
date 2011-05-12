(ns build-paper
  (:require [e-paper.storage :as ep])
  (:use [e-paper.authoring :only (clojure-script)])
  (:use [e-paper.execution :only (run-calclet)])
  (:import java.io.File))

(def paper (ep/create (File. "incanter_example.h5")))

(def jars (ep/store-library-references paper "incanter"))

; Input parameters
(ep/create-data paper "frequency" 0.2)
(ep/create-data paper "time" (vec (range 0. 10. 0.1)))

; Computations are run immediately.
(run-calclet
 (clojure-script paper jars
   (ns calc-sine
     (:use [e-paper-runtime.data]))
   (let [time      (read-data "time")
         frequency (read-data "frequency")
         sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
     (create-data "sine" sine))))

; Visualization
(clojure-script paper jars
  (ns view-sine
    (:use [e-paper-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts]))
  (view (xy-plot (read-data "time") (read-data "sine")
                 :x-label "time" :y-label "sine")))

; Table
(clojure-script paper jars
  (ns sine-table
    (:use [e-paper-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts]))
  (view (col-names (conj-cols (read-data "time") (read-data "sine"))
                   ["time" "sine"])))

; REPL is for calclet development
(clojure-script paper jars
  (ns repl
    (:require clojure.main)
    (:require [clj-hdf5.core :as hdf5])
    (:use [e-paper-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts])
    (:use [incanter.stats]))
  (clojure.main/repl))

(ep/close paper)
