(ns e-paper.execution
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.storage :as storage])
  (:require [e-paper.dependencies :as dependencies])
  (:require [e-paper.security :as security])
  (:require [e-paper.utility :as utility])
  (:import e_paper.ExecutablePaperRef)
  (:import java.io.File))

;
; Bind this var to true for getting an execution trace on the console
;
(def ^{:dynamic true} *print-calclet-execution-trace* false)

;
; Run calclets from a paper
;
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
  [calclet arg]
  (if (= \tab (first arg))
    (let [ds-name (subs arg 1)
          ds      (hdf5/lookup calclet ds-name)
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
                              storage/dereference)
                         jar-paths)
        jar-files   (reduce write-jar '() jars)
        temp-files  (concat temp-files jar-files)
        jar-files   (conj jar-files (File. storage/*e-paper-library*
                                           "e-paper-runtime.jar"))]
    (try
      (let [cl  (security/make-class-loader jar-files)
            ccl (.getContextClassLoader (Thread/currentThread))]
        (try
          (.setContextClassLoader (Thread/currentThread) cl)
          (ExecutablePaperRef/setAccessors cl
             (:accessor code)
             (if (isa? (class (:accessor code))
                       ch.systemsx.cisd.hdf5.IHDF5Writer)
               (:accessor code)
               nil))
          (ExecutablePaperRef/setCurrentCalclet cl (:path code))
          (ExecutablePaperRef/initializeDependencyList cl)
          (exec cl)
          (finally
           (ExecutablePaperRef/clearDependencyList cl)
           (ExecutablePaperRef/setCurrentCalclet cl nil)
           (ExecutablePaperRef/setAccessors cl nil nil)
           (.setContextClassLoader (Thread/currentThread) cl))))
      (finally
       (dorun (map #(.delete %) temp-files))))))

(defn run-calclet
  "Run a calclet within an e-paper."
  [calclet]
  (assert (and (hdf5/node? calclet)
               (#{"script-calclet"
                  "program-calclet"}
                (hdf5/read-attribute calclet "e-paper-datatype"))))
  (when *print-calclet-execution-trace*
    (println "Running calclet" (hdf5/path calclet)))
  (security/with-full-permissions
    (if (= (hdf5/read-attribute calclet "e-paper-datatype")
           "program-calclet")
      ; Run program with arguments using the Java calling conventions
      (let [class-name  (hdf5/read
                         (hdf5/get-attribute calclet "jvm-main-class"))
            args        (-> (hdf5/get-attribute calclet "args")
                            hdf5/read
                            pop)
            args        (map #(retrieve-arg calclet %) args)
            temp-files  (filter identity (map second args))
            args        (map first args)
            arg-array   (into-array Object [(into-array String args)])
            empty-array (into-array Class [(class (make-array String 0))])]
        (run-code calclet temp-files
                  (fn [loader]
                    (let [init-class (.loadClass loader class-name)
                          main       (.getDeclaredMethod init-class "main"
                                                         empty-array)]
                      (security/with-restricted-permissions
                        (try
                          (. main invoke nil arg-array)
                          (catch Exception e
                            (println (str "Exception in calclet "
                                          (hdf5/name calclet) ":"))
                            (println (.toString e)))))))))
      ; Run script using the Java script engine mechanism
      (let [engine-name (hdf5/read (hdf5/get-attribute calclet "script-engine"))
            script      (hdf5/read calclet)]
        (run-code calclet '()
                  (fn [loader]
                    (let [manager (javax.script.ScriptEngineManager. loader)
                          engine  (.getEngineByName manager engine-name)]
                      (security/with-restricted-permissions
                        (try
                          (.eval engine script)
                          (catch javax.script.ScriptException e
                            (println (str "Exception in calclet "
                                          (hdf5/name calclet) ":"))
                            (println (.getMessage e)))))))))))
  (when *print-calclet-execution-trace*
    (println "Calclet" (hdf5/path calclet) "done")))

;
; Rebuild data by running calclets
;
(defn- calclets-for-items
  [paper items]
  (set (for [item items]
         (hdf5/read-attribute item "e-paper-generating-calclet"))))

(defn update
  [paper changed-items]
  (assert (set? changed-items))
  (letfn [(do-update [paper changed-items run-calclets]
            (when (seq changed-items)
              (let [next-generation (dependencies/dependents
                                         paper changed-items)
                    calclets        (calclets-for-items paper next-generation)
                    calclets        (clojure.set/difference
                                          calclets run-calclets)]
                (doseq [calclet calclets]
                  (run-calclet (hdf5/get-dataset paper calclet)))
                (do-update paper next-generation
                           (clojure.set/union run-calclets calclets)))))]
    (do-update paper changed-items #{})))

(defn rebuild-from-primary-items
  [paper clone-file]
  (let [[primary & recalc] (dependencies/dependency-hierarchy paper)
        clone (storage/create clone-file)]
    (try
      (doseq [item primary]
        (hdf5/create-dataset clone (subs (hdf5/path item) 1) item))
      (doseq [items recalc]
        (doseq [calclet (calclets-for-items paper items)]
          (run-calclet (hdf5/get-dataset clone calclet))))
      (finally (storage/close clone)))))

