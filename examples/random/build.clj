; This example illustrates:
;
; 1) how to work with random numbers, by storing the seed of the generator
; 2) how to read and write datasets in small chunks
; 3) how to mix scripts in different languages
;
; Note that for simplicity the random number sequence is read and
; written number by number. It would be more efficient to read and
; write in reasonably sized chunks.

(ns build-random-example
  (:require [e-paper.storage :as ep])
  (:use [e-paper.authoring :only (script, clojure-script)])
  (:use [e-paper.execution :only (run-calclet)]))

; Create an empty e-paper
(def paper (ep/create (java.io.File. "random_numbers.h5")))

; Add references to the jar files from tje libraries used by the calclets
(def jython-jars (ep/store-library-references paper "jython"))
(def incanter-jars (ep/store-library-references paper "incanter"))

; Define input parameters
(ep/create-data paper "random_seed" 42)
(ep/create-data paper "sample_size" 1000)

; Generate a sequence of random numbers in Python
(run-calclet
 (script paper "draw_random_numbers" "python" jython-jars
"
from e_paper_data import readData, createData, writeData, array_spec
import random

random.seed(readData(\"random_seed\"))
nsample = readData(\"sample_size\")

createData(\"random_numbers\", array_spec(float, (nsample,)))
for i in xrange(nsample):
    writeData(\"random_numbers\", [random.gauss(1., 4.)], (i,))
"))

; Calculate average and standard deviation in Python
(run-calclet
 (script paper "calc_statistics" "python" jython-jars
"
from e_paper_data import readData, createData
import HDF5
import math

class mean_variance_accumulator(object):

    def __init__(self):
        self.n = 0
        self.mean = 0.
        self.variance = 0.

    def addValue(self, x):
        d = x - self.mean
        self.mean += d/(self.n+1)
        if self.n > 0:
            self.variance = (self.variance*(self.n-1)+d*(x-self.mean))/self.n
        self.n += 1

nsample = readData(\"sample_size\")

acc = mean_variance_accumulator()
for i in xrange(nsample):
  acc.addValue(readData(\"random_numbers\", (i,), (1,))[0])

createData(\"average\", acc.mean)
createData(\"standard_deviation\", math.sqrt(acc.variance))
"))

; Show a histogram using Incanter
(clojure-script paper incanter-jars
  (ns view-histogram
    (:use [e-paper-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts]))
  (view (histogram (read-data "random_numbers"))))

; Python Console and Clojure REPL for testing
(ep/store-program paper "console" jython-jars "org.python.util.jython" [])

(clojure-script paper incanter-jars
  (ns repl
    (:require clojure.main)
    (:require [clj-hdf5.core :as hdf5])
    (:use [e-paper-runtime.data])
    (:use [incanter.core])
    (:use [incanter.charts])
    (:use [incanter.stats]))
  (clojure.main/repl))

; Close the e-paper file
(ep/close paper)
