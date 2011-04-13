(ns e-paper.data
  (:require [clj-hdf5.core :as hdf5])
  (:import ExecutablePaperRef)
  (:import clj-hdf5.core.hdf-node))

(defn- ds-name [name] (str "/data/" name))

(defn get-dataset
  [name]
  (let [reader (ExecutablePaperRef/getReader)]
    (when (nil? reader)
      (throw (Exception. "no e-paper open")))
    (new hdf-node reader (ds-name name))))

(defn create-dataset
  [name data]
  (let [writer (ExecutablePaperRef/getWriter)]
    (when (nil? writer)
      (throw (Exception. 
              (if (nil? (ExecutablePaperRef/getReader))
                "no e-paper open"
                "e-paper not writable"))))
    (hdf5/create-dataset (new hdf-node writer "/data") name data)))
