(ns inspect-file
  (:require [e-paper.hdf5 :as hdf5]))

(def root (hdf5/open "/Users/hinsen/projects/sputnik/data_model/examples/test.h5"))
(def conf (hdf5/lookup root "conf"))
(def positions (hdf5/lookup root "conf/positions"))

(def universe-ref (first (hdf5/read-attribute conf "universe")))

(hdf5/retrieve-object-from-ref root universe-ref)

(hdf5/close root)
