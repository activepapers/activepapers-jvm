(ns build-python-example
  (:require [e-paper.storage :as ep])
  (:use [e-paper.authoring :only (script)])
  (:use [e-paper.execution :only (run-calclet)]))

; Create an empty e-paper
(def paper (ep/create (java.io.File. "python_example.h5")))

; Add references to the jar files from two libraries used by the calclets
(def jars (ep/store-library-references paper "jython"))
(def plotting-jars (ep/store-library-references paper "jython-plotting"))

; Store input parameters for calclets
(ep/create-data paper "frequency" 0.2)
(ep/create-data paper "time" (vec (range 0. 10. 0.1)))

; Define and run a computational calclet. The name of the calclet
; is "calc-sine" and it is defined as a script using the scripting
; engine called "python" which is part of the Jython standard library.
; The jars define the Java classpath for the execution of the script.

; The runtime library records that this calclet reads "time" and
; "frequency" and creates "sine".  This information is used to create
; the dependency graph.
(run-calclet
 (script paper "calc-sine" "python" jars
"
from e_paper_data import readData, createData
import math

frequency = readData(\"frequency\")
time = readData(\"time\")

sine = [math.sin(2.*math.pi*frequency*t) for t in time]

createData(\"sine\", sine)
"))

; Define (but do not run) a visualization calclet. It creates a simple
; line plot of "time" vs. "sine".
(script paper "view-sine" "python" (vec (concat jars plotting-jars))
"
from e_paper_data import readData, createData
from e_paper_plotting import plot

plot(\"Sine curve\", \"time\", \"sine\",
     (\"sine(x)\", zip(readData(\"time\"), readData(\"sine\"))))

")

; Define a calclet that runs a Python console in the
; environment of this e-paper. The console allows
; interactive data analysis and line-by-line development
; of calclets.
(ep/store-program paper "console" jars "org.python.util.jython" [])

; Close the e-paper file.
(ep/close paper)
