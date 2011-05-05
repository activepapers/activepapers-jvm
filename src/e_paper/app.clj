(ns e-paper.app
  (:gen-class)
  (:require clojure.main)
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.storage :as storage])
  (:require [e-paper.dependencies :as deps])
  (:require [e-paper.execution :as run])
  (:import java.io.File))

(defn error-message
  [& items]
  (.println System/err (apply str items))
  (throw (Exception.)))

(defn assert-file-exists
  [filename]
  (when-not [(.exists (File. filename))]
    (error-message "File " filename " not found")))

(defn run-calclet
  [filename calclet-name]
  (assert-file-exists filename)
  (let [paper   (storage/open (File. filename) :read-write)
        calclet (storage/get-code paper calclet-name)]
    (when (nil? calclet)
      (error-message "No calclet " calclet-name))
    (run/run-calclet calclet)))

(defn rebuild
  [in-filename out-filename]
  (assert-file-exists in-filename)
  (run/rebuild-from-primary-items (storage/open (File. in-filename))
                                  (File. out-filename)))

(defn script
  [filename]
  (assert-file-exists filename)
  (try
    (clojure.main/main filename)
    (catch Exception e
      (println "Exception in script:")
      (println (.toString e)))))

(defn analyze
  [filename]
  (assert-file-exists filename)
  (let [paper (storage/open (File. filename))]

    (when-let [non-e-paper (deps/non-e-paper-items paper)]
      (println "HDF5 items not handled by e-paper:")
      (doseq [node non-e-paper]
        (println (str "  " (:path node)))))
    
    (when-let [libraries (deps/library-dependencies paper)]
      (println "Library dependencies:")
      (doseq [name (keys libraries)]
        (println (str "  " name))
        (doseq [ref (get libraries name)]
          (println (str "    " (hdf5/path ref))))))

    (doseq [[level items] (map vector
                             (iterate inc 0)
                             (deps/dependency-hierarchy paper))]
      (if (zero? level)
        (do
          (println "Items without dependencies:")
          (doseq [node items]
            (when-not (storage/reference? node)
              (println (str "  " (:path node))))))
        (do
          (println "Dependency level " level "")
          (doseq [node items]
            (println (str "  " (:path node)))
            (let [calclet (hdf5/read (hdf5/get-attribute node
                                         "e-paper-generating-calclet"))
                  dependencies (filter (fn [n] (not (= calclet n)))
                                       (map hdf5/path (deps/dependencies node)))]
              (println (str "    dependencies: "
                            (clojure.string/join ", " dependencies)))
              (println (str "    calclet: "
                            calclet))))
          )))))

(def commands
     {"repl"    [clojure.main/main 0]
      "analyze" [analyze 1]
      "rebuild" [rebuild 2]
      "script"  [script 1]
      "run_calclet"     [run-calclet 2]})

(def help-text
"Commands:

analyze <e-paper>
  show the dependencies between the datasets in <e-paper>

rebuild <e-paper> <rebuilt-e-paper>
  copies all non-dependent items from <e-paper> to <rebuilt-e-paper>
  and runs the calclets to rebuild the dependent items

run_calclet <e-paper> <calclet-name>
  run the named calclet from the e-paper
  (provide the calclet name without the prefix /code)

repl
   start a Clojure repl with the e-paper classpath

script
  run a Clojure script in the e-paper environment, usually to
  create an e-paper
")

(defn -main [& args]
  (let [[command-name & args] args
        [command nargs]       (commands command-name)]
    (cond
     (nil? command)
       (println help-text)
     (not= (count args) nargs)
       (println (str command-name " needs " nargs " arguments"))
     :else
       (try
         (apply command args)
         (catch Exception e nil)) )))
