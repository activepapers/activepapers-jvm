(ns e-paper.storage
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.utility :as utility])
  (:require [e-paper.security :as security])
  (:import java.io.File)
  (:import e_paper.ExecutablePaperRef))

;
; Location of the library
;
(def *e-paper-library* (File. (System/getenv "EPAPER_LIBRARY")))

(defn library-file
  "Return a File object for library-name"
  [library-name]
  (File. *e-paper-library* (str library-name ".h5")))

;
; Opening and closing
;
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

;
; References
;
(defn reference?
  [ds]
  (when-let [attr (hdf5/get-attribute ds "e-paper-datatype")]
    (= (hdf5/read attr) "reference")))


; TODO error checking
; TODO will the library file ever be closed?
(defn dereference
  [ds]
  (if (reference? ds)
    (let [[library path] (hdf5/read ds)
          library        (library-file library)]
      (recur (hdf5/lookup (hdf5/open library) path)))
    ds))

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
    (for [ds-name code-nodes]
      (store-code-reference paper ds-name library ds-name))))

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
  [paper name script-file script-engine jars]
  (assert (isa? (class script-file) java.io.File))
  (let [script (slurp script-file)
        ds     (hdf5/create-dataset (hdf5/lookup paper "code")
                                    name script)]
    (hdf5/create-attribute ds "e-paper-datatype" "script")
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
    (hdf5/create-attribute program "e-paper-datatype" "program")
    (hdf5/create-attribute program "jvm-main-class" main-class-name)
    ; Add an empty string to make sure the arg list is never empty,
    ; because HDF5 cannot handle empty arrays.
    (hdf5/create-attribute program "args" (conj (vec args) ""))
    (hdf5/create-attribute program "jvm-jar-files" (map hdf5/path jars))
    program))
    
;
; Run code from a paper
;
(defn get-code
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

(defn- write-script
  [ds]
  (let [script (hdf5/read ds)
        _      (assert (string? script))]
    (utility/create-tempfile "ep-script-" "" script)))

(defn- retrieve-arg
  [program arg]
  (if (= \tab (first arg))
    (let [ds-name (subs arg 1)
          ds      (hdf5/lookup program ds-name)
          data    (hdf5/read ds)
          name    (last (clojure.string/split ds-name #"/"))
          tf      (utility/create-tempfile (str "ep-" name "-") "" data)]
      [(.getAbsolutePath tf) tf])
    [arg nil]))

(defn- run-code
  [code temp-files exec]
  (let [jar-paths   (-> (hdf5/get-attribute code "jvm-jar-files")
                        hdf5/read)
        get-ds      (partial hdf5/get-dataset (hdf5/root code))
        jars        (map #(-> %
                              get-ds
                              dereference)
                         jar-paths)
        jar-files   (reduce write-jar '() jars)
        temp-files  (concat temp-files jar-files)]
    (try
      (let [cl  (security/make-class-loader jar-files)
            ccl (.getContextClassLoader (Thread/currentThread))]
        (try
          (.setContextClassLoader (Thread/currentThread) cl)
          (ExecutablePaperRef/setAccessors
             (:accessor code)
             (if (isa? (class (:accessor code))
                       ch.systemsx.cisd.hdf5.IHDF5Writer)
               (:accessor code)
               nil))
          (ExecutablePaperRef/setCurrentProgram (:path code))
          (ExecutablePaperRef/initializeDependencyList)
          (exec cl)
          (finally
           (ExecutablePaperRef/clearDependencyList)
           (ExecutablePaperRef/setCurrentProgram nil)
           (ExecutablePaperRef/setAccessors nil nil)
           (.setContextClassLoader (Thread/currentThread) cl))))
      (finally
       (dorun (map #(.delete %) temp-files))))))

(defn run-program
  [program]
  (assert (hdf5/group? program))
  (security/with-full-permissions
    (let [class-name  (hdf5/read (hdf5/get-attribute program "jvm-main-class"))
          args        (-> (hdf5/get-attribute program "args")
                          hdf5/read
                          pop)
          args        (map #(retrieve-arg program %) args)
          temp-files  (filter identity (map second args))
          args        (map first args)
          arg-array   (into-array Object [(into-array String args)])
          empty-array (into-array Class [(class (make-array String 0))])]
      (run-code program temp-files
                (fn [loader]
                  (let [init-class (.loadClass loader class-name)
                        main       (.getDeclaredMethod init-class "main"
                                                       empty-array)]
                    (security/with-restricted-permissions
                      (. main invoke nil arg-array))))))))

(defn run-script
  [script]
  (assert (hdf5/node? script))
  (security/with-full-permissions
    (let [engine-name (hdf5/read (hdf5/get-attribute script "script-engine"))
          script-text (hdf5/read script)]
      (run-code script '()
                (fn [loader]
                  (let [manager (javax.script.ScriptEngineManager. loader)
                        engine  (.getEngineByName manager engine-name)]
                    (security/with-restricted-permissions
                      (.eval engine script-text))))))))

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
