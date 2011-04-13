(ns generate-input
  (:require [e-paper.data :as data]))

(data/create-dataset "time" (vec (range 0. 10. 0.1)))
(data/create-dataset "frequency" 0.2)

