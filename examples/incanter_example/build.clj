(ns incanter-example.build
  (:require [active-papers.storage :as ep])
  (:use [active-papers.authoring :only (clojure-script)])
  (:use [active-papers.execution :only (run-calclet)]))

; Create an empty active paper
(def paper (ep/create (java.io.File. "incanter_example.h5")))

; Add references to the jar files from the Incanter library
(def jars (ep/store-library-references paper "incanter"))

; Store input parameters for calclets
(ep/create-data paper "frequency" 0.2)
(ep/create-data paper "time" (vec (range 0. 10. 0.1)))

; Define and run a computational calclet. The clojure-script function
; knows how to run Clojure scripts and takes the name of the calclet
; (calc-sine) from the Clojure namespace declaration. The jars define
; the Java classpath for the execution of the script.

; The runtime library records that this calclet reads "time" and
; "frequency" and creates "sine".  This information is used to create
; the dependency graph.
(run-calclet
 (clojure-script paper jars
   (ns calc-sine
     (:use [active-paper-clojure-runtime.data]))
   (let [time      (read-data "time")
         frequency (read-data "frequency")
         sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
     (create-data "sine" sine))))

; Define (but do not run) a visualization calclet. It creates a simple
; line plot of "time" vs. "sine".
(clojure-script paper jars
  (ns view-sine
    (:use [active-paper-clojure-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts]))
  (view (xy-plot (read-data "time") (read-data "sine")
                 :x-label "time" :y-label "sine")))

; Define (but do not run) a visualization calclet. It a tabular
; representation of "time" vs. "sine".
(clojure-script paper jars
  (ns sine-table
    (:use [active-paper-clojure-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts]))
  (view (col-names (conj-cols (read-data "time") (read-data "sine"))
                   ["time" "sine"])))

; Define a calclet that runs a Clojure read-eval-print loop (REPL) in
; the environment of this active paper. The REPL allows interactive data
; analysis and line-by-line development of calclets.
(clojure-script paper jars
  (ns repl
    (:require clojure.main)
    (:require [clj-hdf5.core :as hdf5])
    (:use [active-paper-clojure-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts])
    (:use [incanter.stats]))
  (clojure.main/repl))

; Close the active paper file.
(ep/close paper)
