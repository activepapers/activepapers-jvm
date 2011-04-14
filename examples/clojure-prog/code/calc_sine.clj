(ns calc-sine
  (:require [e-paper-runtime.data :as data])
  (:require [clj-hdf5.core :as hdf5]))

(let [time      (hdf5/read (data/get-dataset "time"))
      frequency (hdf5/read (data/get-dataset "frequency"))
      sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
  (data/create-dataset "sine" sine))
