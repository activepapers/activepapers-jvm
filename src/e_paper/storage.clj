(ns e-paper.storage
  (:require [e-paper.jhdf5 :as hdf5])
  (:require [e-paper.utility :as utility]))

; This information should be taken from an environment variable or
; a configuration file!
(def *e-paper-library* "/Users/hinsen/projects/e-paper/e-paper-library")

(defn create
  [filename]
  (let [root (hdf5/create filename)]
    (hdf5/create-attribute root "DATA_MODEL" "e-paper")
    (hdf5/create-attribute root "DATA_MODEL_MAJOR_VERSION" 0)
    (hdf5/create-attribute root "DATA_MODEL_MINOR_VERSION" 1)
    (hdf5/create-group root "code")
    (hdf5/create-group root "data")
    (hdf5/create-group root "text")
    root))

(def open hdf5/open)

(def close hdf5/close)

(defn store-jar
  [paper ds-name jar-filename]
  (let [code (hdf5/lookup paper "code")]
    (hdf5/create-dataset
       code ds-name {:tag "jar" :data (utility/read-file jar-filename)})))

(defn store-code-reference
  [paper ds-name library path]
  (let [code (hdf5/lookup paper "code")
        ds   (hdf5/create-dataset code ds-name [library path])]
    (hdf5/create-attribute ds "e-paper-datatype" "reference")
    ds))

;; (defn store-program
;;   [paper ds-name script-filename jars main-class-name args]
;;   (let [script (if (nil? script-filename) "" (slurp script-filename))
;;         args   (map (fn [a] (if (= a :script-filename) "" a)) args)
;;         ds (hdf5/create-string-dataset (hdf5/lookup paper "code")
;;                                        ds-name script)]
;;     (hdf5/write ds (into-array String [script]))
;;     (hdf5/create-string-attribute ds "jvm-main-class" [main-class-name])
;;     ; Add an empty string because HDF5 cannot handle empty arrays.
;;     (hdf5/create-string-attribute ds "jvm-args" (conj (vec args) ""))
;;     (hdf5/create-reference-attribute ds "jvm-jar-files" jars)
;;     ds))

(defn store-script
  [paper ds-name script-filename script-engine jars]
  (let [script (slurp script-filename)
        ds     (hdf5/create-dataset (hdf5/lookup paper "code")
                                    ds-name script)]
    (hdf5/create-attribute ds "script-engine" script-engine)
    (hdf5/create-attribute ds "jvm-jar-files" (map hdf5/path jars))
    ds))

(defn reference?
  [ds]
  (when-let [attr (hdf5/get-attribute ds "e-paper-datatype")]
    (= (hdf5/read attr) "reference")))

; TODO error checking
; TODO will the library file ever be closed?
(defn dereference
  [ds]
  (if (reference? ds)
    (let [[library code-path] (hdf5/read ds)
          library             (str *e-paper-library* "/" library ".h5")
          path                (str "code/" code-path)]
      (hdf5/lookup (hdf5/open library) path))
    ds))

(defn get-program
  [paper name]
  (dereference (hdf5/lookup (hdf5/lookup paper "code") name)))

(defn- write-jar
  [tempfiles ds]
  (let [{tag :tag data :data} (hdf5/read ds)
        _   (assert (= tag "jar"))
        tf  (utility/create-tempfile "ep-" ".jar" data)]
    (cons tf tempfiles)))

(defn- write-script
  [ds]
  (let [script (hdf5/read ds)
        _      (assert (string? script))]
    (utility/create-tempfile "ep-" "" script)))

;; (defn run-program
;;   [script]
;;   (let [class-name   (first (hdf5/read-attribute script "jvm-main-class"))
;;         args         (into-array String
;;                                  (pop (hdf5/read-attribute script "jvm-args")))
;;         jars         (map #(dereference (hdf5/retrieve-object-from-ref
;;                                          (hdf5/lookup script "/") %))
;;                           (hdf5/read-attribute script "jvm-jar-files"))
;;         jar-files    (reduce write-jar '() jars)
;;         script-file  (write-script script)
;;         temp-files   (cons script-file jar-files)
;;         args         (map (fn [a]
;;                             (if (empty? a)
;;                               (.getAbsolutePath script-file)
;;                               a))
;;                           args)]
;;     (try
;;       (let [loader      (new java.net.URLClassLoader
;;                              (into-array (map #(.toURL %) jar-files)))
;;             init-class  (.loadClass loader class-name)
;;             main        (.getDeclaredMethod init-class "main"
;;                            (into-array Class [(class (make-array String 0))]))]
;;         (. main invoke nil (into-array Object [(into-array String args)])))
;;       (finally
;;        (dorun (map #(.delete %) temp-files))))))

(defn run-script
  [script]
  (let [engine-name (hdf5/read (hdf5/get-attribute script "script-engine"))
        script-text (hdf5/read script)
        jar-paths   (-> (hdf5/get-attribute script "jvm-jar-files")
                        hdf5/read)
        get-ds      (partial hdf5/lookup (hdf5/root script))
        jars        (map #(-> %
                              (subs 1)
                              get-ds
                              dereference)
                         jar-paths)
        jar-files   (reduce write-jar '() jars)]
    (try
      (let [loader      (new java.net.URLClassLoader
                             (into-array java.net.URL
                                         (map #(.toURL %) jar-files)))
            manager     (new javax.script.ScriptEngineManager loader)
            engine      (.getEngineByName manager engine-name)]
        (.eval engine script-text))
      (finally
       (dorun (map #(.delete %) jar-files))))))
