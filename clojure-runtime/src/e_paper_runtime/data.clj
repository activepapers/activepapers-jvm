(ns e-paper-runtime.data
 (:require [clj-hdf5.core :as hdf5])
  (:import e_paper.ExecutablePaperRef)
  (:import e_paper_runtime.HDF5Node)
  (:import e_paper_runtime.DataAccess)
  (:import java.io.File))

;
; Access to an existing dataset, with reference handling.
;
(defn get-data
  [name]
  (let [node (DataAccess/getData name)]
    (hdf5/make-hdf-node (.getReader node) (.getPath node))))

(defn read-data
  [name]
  (let [node (DataAccess/getData name)]
    (hdf5/read (hdf5/make-hdf-node (.getReader node) (.getPath node)))))

                                        ;
; Creation of a new dataset, with all the required attributes.
;
(defn create-data
  [name data]
  (let [node (DataAccess/createData name)
        clj-node (hdf5/make-hdf-node (.getWriter node) (.getPath node))
        group (hdf5/parent clj-node)
        ds (hdf5/create-dataset group name data)]
    (DataAccess/finalizeData node)
    ds))

