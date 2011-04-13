(ns hello
  (:require [e-paper.data :as data])
  (:require [clj-hdf5.core :as hdf5]))

(prn (hdf5/read (data/get-dataset "message")))
