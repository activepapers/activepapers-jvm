(ns e-paper.data
  (:require [clj-hdf5.core :as hdf5])
  (:import ExecutablePaperRef)
  (:import clj-hdf5.core.hdf-node))

(defn get-dataset
  [name]
  (let [reader (ExecutablePaperRef/getReader)]
    (when (nil? reader)
      (throw (Exception. "no e-paper open")))
    (new hdf-node reader (str "data/" name))))
