(ns e-paper.app
  (:gen-class)
  (:require [clojure.main])
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.storage :as storage])
  (:require [e-paper.dependencies :as deps])
  (:require [e-paper.execution :as run])
  (:import java.io.File))

(defn error-exit
  [& items]
  (.println System/err (apply str items))
  (throw (Exception.)))

(defn assert-file-exists
  [filename]
  (when-not [(.exists (File. filename))]
    (error-exit "File " filename " not found")))

(defn print-ds
  [filename dataset-name]
  (assert-file-exists filename)
  (let [paper (storage/open (File. filename) :read-write)
        ds    (storage/get-data paper dataset-name)]
    (when (nil? ds)
      (error-exit "No dataset " dataset-name))
    (println (hdf5/read ds))))

(defn run-calclet
  [filename calclet-name]
  (assert-file-exists filename)
  (let [paper   (storage/open (File. filename) :read-write)
        calclet (storage/get-code paper calclet-name)]
    (when (nil? calclet)
      (error-exit "No calclet " calclet-name))
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
        (println (str "  " (hdf5/path node)))))
    
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
              (println (str "  " (hdf5/path node))))))
        (do
          (println "Dependency level " level "")
          (doseq [node items]
            (println (str "  " (hdf5/path node)))
            (let [calclet (hdf5/read (hdf5/get-attribute node
                                         "e-paper-generating-calclet"))
                  dependencies (filter (fn [n] (not (= calclet n)))
                                       (map hdf5/path (deps/dependencies node)))]
              (println (str "    dependencies: "
                            (clojure.string/join ", " dependencies)))
              (println (str "    calclet: "
                            calclet))))
          )))))

(defn parse-jar-spec
  [jar-spec]
  (let [[_ ds-name jar-file]
        (re-matches #"([a-zA-Z0-9-_]+)=(.*\.jar)" jar-spec)]
    (if (nil? ds-name)
      (error-exit "invalid argument: " jar-spec)
      [ds-name jar-file])))

(defn make-library
  [library-filename & jar-specs]
  (let [jar-specs (map parse-jar-spec jar-specs)
        library (storage/create (File. library-filename))]
    (doseq [[ds-name jar-file] jar-specs]
      (storage/store-jar library ds-name (File. jar-file)))
    (storage/close library)))

(defn parse-dataset-spec
  [dataset-spec]
  (let [regexps [[#"([a-zA-Z0-9-_/]+)=(-?[0-9]+)"
                  #(Long/parseLong %)]
                 [#"([a-zA-Z0-9-_/]+)=(-?[0-9]+\.[0-9]*)"
                  #(Double/parseDouble %)]
                 [#"([a-zA-Z0-9-_/]+)=\'(.*)\'"
                  identity]
                 [#"([a-zA-Z0-9-_/]+)=\"(.*)\""
                  identity]
                 [#"([a-zA-Z0-9-_/]+)=(\[-?[0-9]+([ ]+-?[0-9]+)*\])"
                  (fn [s] (vec (map #(Long/parseLong %)
                                   (clojure.string/split
                                    (subs s 1 (dec (count s))) #"[ ]+"))))]
                 [#"([a-zA-Z0-9-_/]+)=(\[-?[0-9]+(\.[0-9]*)?([ ]+-?[0-9]+(\.[0-9]*)?)*\])"
                  (fn [s] (vec (map #(Double/parseDouble %)
                                   (clojure.string/split
                                    (subs s 1 (dec (count s))) #"[ ]+"))))]]
        parse   (fn [[regexp convert]]
                  (when-let [[_ name value]
                             (re-matches regexp dataset-spec)]
                    [name (convert value)]))
        match   (some identity (map parse regexps))]
    (when (nil? match)
      (error-exit "invalid syntax: " dataset-spec))
    match))

(defn update
  [filename & dataset-specs]
  (assert-file-exists filename)
  (let [paper (storage/open (File. filename) :read-write)]
    (run/update paper 
      (set (for [[ds-name value] (map parse-dataset-spec dataset-specs)]
             (let [ds (storage/get-data paper ds-name)]
               (when (nil? ds)
                 (error-exit "Dataset " ds-name " not found"))
               (when-let [creator (hdf5/read-attribute
                                   ds "e-paper-generating-calclet")]
                 (when (pos? (count creator))
                   (error-exit "Dataset " ds-name
                               " owned by calclet " creator)))
               (storage/create-data paper ds-name value)))))))

; Just requiring swank.swank causes a severe delay at the end of the
; program, so do this only when the swank server is really requested.
(defn swank-server
  []
  (eval '(do
           (require 'swank.swank)
           (swank.swank/start-repl))))

(def commands
     {"repl"          [clojure.main/main 0]
      "swank-server"  [swank-server 0]
      "analyze"       [analyze 1]
      "rebuild"       [rebuild 2]
      "script"        [script 1]
      "run_calclet"   [run-calclet 2]
      "update"        [update [1 nil]]
      "make_library"  [make-library [1 nil]]
      "print"         [print-ds 2]})

(def help-text
"Commands:

analyze <e-paper>
  show the dependencies between the datasets in <e-paper>

make_library <e-paper> <jar-spec> ...
  creates an e-paper representing a code library from a
  collection of jar files. Each jar-spec has the form
  name=jar_file_name, where name is the dataset name in
  the code section of the e-paper.

print <e-paper> <dataset>
  prints the contents of a dataset

rebuild <e-paper> <rebuilt-e-paper>
  copies all non-dependent items from <e-paper> to <rebuilt-e-paper>
  and runs the calclets to rebuild the dependent items

run_calclet <e-paper> <calclet-name>
  run the named calclet from the e-paper
  (provide the calclet name without the prefix /code)

repl
   start a Clojure repl with the e-paper classpath
   (This is a development tool.)

script
  run a Clojure script in the e-paper environment, usually to
  create an e-paper

swank-server
   start a swank server with the e-paper classpath
   (This is a development tool.)

update <e-paper> <dataset>=<value> ...
  updates the specified datasets and runs all calclets required
  to re-calculate dependent datasets
")

(defn -main [& args]
  (let [[command-name & opts-and-args] args
        {opts true args false} (group-by #(= \- (first %)) opts-and-args)
        [command nargs]        (commands command-name)
        [min_args max_args]    (if (number? nargs) [nargs nargs] nargs)]
    (cond
     (nil? command)
       (println help-text)
     (or (and (number? min_args) (< (count args) min_args))
         (and (number? max_args) (> (count args) max_args)))
       (println (str command-name " needs " min_args
                                  " to " max_args  " arguments"))
     :else
       (try
         (binding [run/*print-calclet-execution-trace*
                     (contains? (set opts) "-t")]
           (apply command args))
         (catch Exception e nil)) )))
