(ns e-paper-runtime.data
  (:require [clj-hdf5.core :as hdf5])
  (:import e_paper.ExecutablePaperRef))

(defn- ds-name [name] (str "/data/" name))

(defn get-dataset
  [name]
  (let [reader (ExecutablePaperRef/getReader)]
    (when (nil? reader)
      (throw (Exception. "no e-paper open")))
    (hdf5/make-hdf-node reader (ds-name name))))

(defn create-dataset
  [name data]
  (let [writer (ExecutablePaperRef/getWriter)]
    (when (nil? writer)
      (throw (Exception. 
              (if (nil? (ExecutablePaperRef/getReader))
                "no e-paper open"
                "e-paper not writable"))))
    (hdf5/create-dataset (hdf5/make-hdf-node writer "/data") name data)))
