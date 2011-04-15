(ns e-paper-runtime.data
  (:require [clj-hdf5.core :as hdf5])
  (:import e_paper.ExecutablePaperRef)
  (:import java.io.File))

; This file duplicates some code from e-paper.storage for now.

;
; Location of the library
;
(def *e-paper-library* (File. (System/getenv "EPAPER_LIBRARY")))

(defn library-file
  "Return a File object for library-name"
  [library-name]
  (File. *e-paper-library* (str library-name ".h5")))

;
; References
;
(defn- reference?
  [ds]
  (when-let [attr (hdf5/get-attribute ds "e-paper-datatype")]
    (= (hdf5/read attr) "reference")))

(defn- dereference
  [ds]
  (if (reference? ds)
    (let [[library path] (hdf5/read ds)
          library        (library-file library)]
      (recur (hdf5/lookup (hdf5/open library) path)))
    ds))

;
; Access to an existing dataset, with reference handling.
;
(defn get-data
  [name]
  (let [reader     (ExecutablePaperRef/getReader)
        root       (hdf5/make-hdf-node reader "/")
        data-group (hdf5/lookup root "data")]
    (when (nil? reader)
      (throw (Exception. "no e-paper open")))
    (let [ds (hdf5/lookup data-group name)]
      (ExecutablePaperRef/addDependency (:path ds))
      (dereference ds))))

;
; Creation of a new dataset, with all the required attributes.
;
(defn create-data
  [name data]
  (let [writer     (ExecutablePaperRef/getWriter)
        root       (hdf5/make-hdf-node writer "/")
        data-group (hdf5/lookup root "data")
        program    (ExecutablePaperRef/getCurrentProgram)]
    (when (nil? writer)
      (throw (Exception. 
              (if (nil? (ExecutablePaperRef/getReader))
                "no e-paper open"
                "e-paper not writable"))))
    (when-let [ds (hdf5/lookup data-group name)]
      (let [creator (hdf5/get-attribute ds "e-paper-generating-program")]
        (if (or (nil? creator) (not= (hdf5/read creator) program))
          (throw (Exception. (str "Overwriting dataset " name " created by "
                                  "program " (hdf5/read creator)))))))
    (let [ds   (hdf5/create-dataset data-group name data)
          deps (vec (.toArray (ExecutablePaperRef/getDependencyList)
                              (make-array String 0)))
          ; HDF5 dosn't like empty arrays
          deps (if (empty? deps) [""] deps)]
      (hdf5/create-attribute ds "e-paper-generating-program" program)
      (hdf5/create-attribute ds "e-paper-dependencies" deps)
      ds)))
