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

(comment
  (defn get-data
    [name]
    (let [reader     (ExecutablePaperRef/getReader)
          root       (hdf5/make-hdf-node reader "/")
          data-group (hdf5/lookup root "data")]
      (when (nil? reader)
        (throw (Exception. "no e-paper open")))
      (let [ds (hdf5/lookup data-group name)]
        (ExecutablePaperRef/addDependency (:path ds))
        (dereference ds)))))

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

(comment
  (defn create-data
    [name data]
    (let [writer  (ExecutablePaperRef/getWriter)
          calclet (ExecutablePaperRef/getCurrentCalclet)]
      (when (nil? writer)
        (throw (Exception. 
                (if (nil? (ExecutablePaperRef/getReader))
                  "no e-paper open"
                  "e-paper not writable"))))
      (when (nil? calclet)
        (throw (Exception. "no calclet active")))
      (let [root       (hdf5/make-hdf-node writer "/")
            data-group (hdf5/lookup root "data")]
        (when-let [ds (hdf5/lookup data-group name)]
          (let [creator (hdf5/get-attribute ds "e-paper-generating-calclet")]
            (if (or (nil? creator) (not= (hdf5/read creator) calclet))
              (throw (Exception. (str "Overwriting dataset " name " created by "
                                      "calclet " (hdf5/read creator)))))))
        (let [ds   (hdf5/create-dataset data-group name data)
                                        ; all datasets read by the calclet so far are dependencies
              deps (vec (.toArray (ExecutablePaperRef/getDependencyList)
                                  (make-array String 0)))
                                        ; add the calclet itself as a dependency
              deps (conj deps calclet)
                                        ; and also add the calclet's jar files
              deps (concat deps (-> (hdf5/lookup root (subs calclet 1))
                                    (hdf5/get-attribute "jvm-jar-files")
                                    (hdf5/read)))]
          (hdf5/create-attribute ds "e-paper-datatype" "data")
          (hdf5/create-attribute ds "e-paper-generating-calclet" calclet)
          (hdf5/create-attribute ds "e-paper-dependencies" deps)
          ds)))))
