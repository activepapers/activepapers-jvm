(ns e-paper.storage
  (:require [e-paper.jhdf5 :as hdf5])
  (:require [e-paper.utility :as utility])
  (:import java.io.File))

; This information should be taken from an environment variable or
; a configuration file!
(def *e-paper-library* (File. "/Users/hinsen/projects/e-paper/e-paper-library"))

(defn create
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

(defn store-jar
  [paper ds-name jar-file]
  (assert (isa? (class jar-file) java.io.File))
  (let [code (hdf5/lookup paper "code")]
    (hdf5/create-dataset
       code ds-name {:tag "jar" :data (utility/read-file jar-file)})))

(defn store-code-reference
  [paper ds-name library path]
  (let [code (hdf5/lookup paper "code")
        ds   (hdf5/create-dataset code ds-name [library path])]
    (hdf5/create-attribute ds "e-paper-datatype" "reference")
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
  [paper name jars main-class-name args]
  (let [code    (hdf5/lookup paper "code")
        program (hdf5/create-group code name)
        args    (map (fn [arg n] (process-program-arg program arg n))
                     args (iterate inc 1))]
    (hdf5/create-attribute program "jvm-main-class" main-class-name)
    ; Add an empty string to make sure the arg list is never empty,
    ; because HDF5 cannot handle empty arrays.
    (hdf5/create-attribute program "args" (conj (vec args) ""))
    (hdf5/create-attribute program "jvm-jar-files" (map hdf5/path jars))
    program))
    
(defn store-script
  [paper name script-file script-engine jars]
  (assert (isa? (class script-file) java.io.File))
  (let [script (slurp script-file)
        ds     (hdf5/create-dataset (hdf5/lookup paper "code")
                                    name script)]
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
          library             (File. *e-paper-library* (str library ".h5"))
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

(defn- write-arg-file
  [tempfiles ds]
  (let [data (hdf5/read ds)
        tf   (utility/create-tempfile "ep-arg-" ".jar" data)]
    (cons tf tempfiles)))

(defn- retrieve-arg
  [program arg]
  (if (= \tab (first arg))
    (let [ds-name (subs arg 1)
          ds      (hdf5/lookup program ds-name)
          data    (hdf5/read ds)
          tf      (utility/create-tempfile "ep-arg-" "" data)]
      [(.getAbsolutePath tf) tf])
    [arg nil]))

(defn run-program
  [program]
  (assert (hdf5/group? program))
  (let [class-name  (hdf5/read (hdf5/get-attribute program "jvm-main-class"))
        args        (-> (hdf5/get-attribute program "args")
                        hdf5/read
                        pop)
        args        (map #(retrieve-arg program %) args)
        temp-files  (filter identity (map second args))
        args        (map first args)
        jar-paths   (-> (hdf5/get-attribute program "jvm-jar-files")
                        hdf5/read)
        get-ds      (partial hdf5/lookup (hdf5/root program))
        jars        (map #(-> %
                              (subs 1)
                              get-ds
                              dereference)
                         jar-paths)
        jar-files   (reduce write-jar '() jars)
        temp-files  (concat temp-files jar-files)]
    (try
      (let [loader      (new java.net.URLClassLoader
                             (into-array (map #(.toURL %) jar-files)))
            init-class  (.loadClass loader class-name)
            main        (.getDeclaredMethod init-class "main"
                           (into-array Class [(class (make-array String 0))]))]
        (. main invoke nil (into-array Object [(into-array String args)])))
      (finally
       (dorun (map #(.delete %) temp-files))))))

(defn- write-script
  [ds]
  (let [script (hdf5/read ds)
        _      (assert (string? script))]
    (utility/create-tempfile "ep-script-" "" script)))

(defn run-script
  [script]
  (assert (hdf5/node? script))
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
                                         (map #(.toURL (.toURI %)) jar-files)))
            manager     (new javax.script.ScriptEngineManager loader)
            engine      (.getEngineByName manager engine-name)]
        (.eval engine script-text))
      (finally
       (dorun (map #(.delete %) jar-files))))))
