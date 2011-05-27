(ns e-paper.storage
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.utility :as utility])
  (:import java.io.File))

;
; Location of the library
;
(let [path (System/getenv "EPAPER_LIBRARY")]
  (when (nil? path)
    (throw (Exception. "Environment variable EPAPER_LIBRARY not set.")))
  (def *e-paper-library* (File. path)))

(defn library-file
  "Return a java.io.File object for library-name"
  [library-name]
  (File. *e-paper-library* (str library-name ".h5")))

;
; Opening and closing
;
(defn create
  "Create a new empty e-paper. A previously existing file of the same
   name is overwritten."
  [file]
  (assert (isa? (class file) java.io.File))
  (let [root (hdf5/create file)]
    (hdf5/create-attribute root "DATA_MODEL" "e-paper")
    (hdf5/create-attribute root "DATA_MODEL_MAJOR_VERSION" 0)
    (hdf5/create-attribute root "DATA_MODEL_MINOR_VERSION" 1)
    (hdf5/create-group root "code")
    (hdf5/create-group root "data")
    (hdf5/create-group root "text")
    root))

(def open hdf5/open)

(def close hdf5/close)

;
; References
;
(defn reference?
  "Return true if ds is a reference to another e-paper."
  [ds]
  (when-let [attr (hdf5/get-attribute ds "e-paper-datatype")]
    (= (hdf5/read attr) "reference")))


; TODO error checking
; TODO will the library file ever be closed?
(defn dereference
  [ds]
  (cond (nil? ds)         nil
        (reference? ds)   (let [[library path] (hdf5/read ds)
                                library        (library-file library)]
                            (recur (hdf5/lookup (hdf5/open library) path)))
        :else              ds))

(defn reference-exists?
  [library path]
  (let [file (library-file library)]
    (if (.exists file)
      (hdf5/node? (hdf5/lookup (hdf5/open file) path))
      false)))

(defn store-code-reference
  [paper ds-name library path]
  (let [path (str "code/" path)]
    (assert (reference-exists? library path))
    (let [code (hdf5/lookup paper "code")
          ds   (hdf5/create-dataset code ds-name [library path])]
      (hdf5/create-attribute ds "e-paper-datatype" "reference")
      ds)))

(defn store-library-references
  [paper library]
  (let [lib-hdf5   (hdf5/open (library-file library))
        code-nodes (-> (hdf5/lookup lib-hdf5 "code") hdf5/members keys)]
    (hdf5/close lib-hdf5)
    (doall (for [ds-name code-nodes]
             (store-code-reference paper ds-name library ds-name)))))

;
; Store code in a paper
;
(defn store-jar
  [paper ds-name jar-file]
  (assert (isa? (class jar-file) java.io.File))
  (let [code (hdf5/lookup paper "code")
        ds   (hdf5/create-dataset code ds-name
                 {:tag "jar" :data (utility/read-file jar-file)})]
    (hdf5/create-attribute ds "e-paper-datatype" "jar")))

(defn store-script
  "Store the script contained in script-file under name in paper
   such that it will be run with script-engine and with the jars
   on the classpath."
  [paper name script-text-or-file script-engine jars]
  (assert (or (string? script-text-or-file)
              (isa? (class script-text-or-file) java.io.File)))
  (let [script (if (string? script-text-or-file)
                 script-text-or-file
                 (slurp script-text-or-file))
        ds     (hdf5/create-dataset (hdf5/lookup paper "code")
                                    name script)]
    (hdf5/create-attribute ds "e-paper-datatype" "script-calclet")
    (hdf5/create-attribute ds "script-engine" script-engine)
    (hdf5/create-attribute ds "jvm-jar-files" (map hdf5/path jars))
    ds))

(defn- process-program-arg
  [program arg number]
  (if (string? arg)
    arg
    (case (:type arg)
      :string
         (:contents arg)
      :text-file
          (let [ds-name (str "arg" number)
                ds (hdf5/create-dataset program ds-name (:contents arg))]
            (str "\t" ds-name) ))))

(defn store-program
  "Store a program under name in paper. The program will be run
   like from the Java command line with args being supplied to
   method main from the class named by main-class-name, and with
   the jars on the classpath."
  [paper name jars main-class-name args]
  (let [code    (hdf5/lookup paper "code")
        program (hdf5/create-group code name)
        args    (map (fn [arg n] (process-program-arg program arg n))
                     args (iterate inc 1))]
    (hdf5/create-attribute program "e-paper-datatype" "program-calclet")
    (hdf5/create-attribute program "jvm-main-class" main-class-name)
    ; Add an empty string to make sure the arg list is never empty,
    ; because HDF5 cannot handle empty arrays.
    (hdf5/create-attribute program "args" (conj (vec args) ""))
    (hdf5/create-attribute program "jvm-jar-files" (map hdf5/path jars))
    program))
    
;
; Access code from a paper
;
(defn get-code
  [paper name]
  (dereference (hdf5/lookup (hdf5/lookup paper "code") name)))

;
; Access to data in the paper
;
(defn get-data
  [paper name]
  (dereference (hdf5/lookup paper (str "data/" name))))

(defn create-data
  [paper name data]
  (let [ds (hdf5/create-dataset (hdf5/lookup paper "data") name data)]
    (hdf5/create-attribute ds "e-paper-datatype" "data")
    (hdf5/create-attribute ds "e-paper-generating-program" "")
    (hdf5/create-attribute ds "e-paper-dependencies" [""])
    ds))
