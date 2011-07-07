(ns active-papers.cltool
  (:gen-class)
  (:require [clojure.main])
  (:require [clojure.string :as str])
  (:require [clj-hdf5.core :as hdf5])
  (:require [active-papers.storage :as storage])
  (:require [active-papers.dependencies :as deps])
  (:require [active-papers.execution :as run])
  (:require [active-papers.security :as security])
  (:import java.io.File))

(defn error-exit
  [& items]
  (.println System/err (apply str items))
  (throw (Exception. "error-exit")))

(defn assert-file-exists
  [filename]
  (when-not [(.exists (File. filename))]
    (error-exit "File " filename " not found")))

(defn open-paper
  [paper mode]
  (cond (string? paper)    (do (assert-file-exists paper)
                               (storage/open (File. paper) mode))
        (hdf5/node? paper) paper
        :else              nil))

(defn print-ds
  [paper dataset-name]
  (let [paper (open-paper paper :read-write)
        ds    (storage/get-data paper dataset-name)]
    (when (nil? ds)
      (error-exit "No dataset " dataset-name))
    (println (hdf5/read ds))))

(defn run-calclet
  [paper calclet-name]
  (let [paper   (open-paper paper :read-write)
        calclet (storage/get-code paper calclet-name)]
    (when (nil? calclet)
      (error-exit "No calclet " calclet-name))
    (run/run-calclet calclet)))

(defn rebuild
  [in-paper out-filename]
  (run/rebuild-from-primary-items (open-paper in-paper :read-only)
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
  [paper]
  (let [paper (open-paper paper :read-only)]

    (when-let [non-active-paper (deps/non-active-paper-items paper)]
      (println "HDF5 items not handled by active-paper:")
      (doseq [node non-active-paper]
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
                                         "active-paper-generating-calclet"))
                  dependencies (filter (fn [n] (not (= calclet n)))
                                       (map hdf5/path (deps/dependencies node)))]
              (println (str "    dependencies: "
                            (str/join ", " dependencies)))
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
                 [#"([a-zA-Z0-9-_/]+)=(\[-?[0-9]+(,-?[0-9]+)*\])"
                  (fn [s] (vec (map #(Long/parseLong %)
                                   (str/split
                                    (subs s 1 (dec (count s))) #","))))]
                 [#"([a-zA-Z0-9-_/]+)=(\[-?[0-9]+(\.[0-9]*)?(,-?[0-9]+(\.[0-9]*)?)*\])"
                  (fn [s] (vec (map #(Double/parseDouble %)
                                   (str/split
                                    (subs s 1 (dec (count s))) #","))))]]
        parse   (fn [[regexp convert]]
                  (when-let [[_ name value]
                             (re-matches regexp dataset-spec)]
                    [name (convert value)]))
        match   (some identity (map parse regexps))]
    (when (nil? match)
      (error-exit "invalid syntax: " dataset-spec))
    match))

(defn update
  [paper & dataset-specs]
  (let [paper (open-paper paper :read-write)]
    (run/update paper 
      (set (for [[ds-name value] (map parse-dataset-spec dataset-specs)]
             (let [ds (storage/get-data paper ds-name)]
               (when (nil? ds)
                 (error-exit "Dataset " ds-name " not found"))
               (when-let [creator (hdf5/read-attribute
                                   ds "active-paper-generating-calclet")]
                 (when (pos? (count creator))
                   (error-exit "Dataset " ds-name
                               " owned by calclet " creator)))
               (storage/create-data paper ds-name value)))))))

; Merely requiring swank.swank causes a severe delay at the end of the
; program, so do this only when the swank server is really requested.
(defn swank-server
  []
  (eval '(do
           (require 'swank.swank)
           (swank.swank/start-repl))))

(def help-text
"Commands:

analyze <active-paper>
  show the dependencies between the datasets in <active-paper>

make_library <active-paper> <jar-spec> ...
  creates an active-paper representing a code library from a
  collection of jar files. Each jar-spec has the form
  name=jar_file_name, where name is the dataset name in
  the code section of the active-paper.

open <active-paper>
  opens active-paper and accepts a sequence of commands to work
  on it. This is faster than running the active-paper tool repeatedly
  for each command.

print <active-paper> <dataset>
  prints the contents of a dataset

rebuild <active-paper> <rebuilt-active-paper>
  copies all non-dependent items from <active-paper> to <rebuilt-active-paper>
  and runs the calclets to rebuild the dependent items

run_calclet <active-paper> <calclet-name>
  run the named calclet from the active-paper
  (provide the calclet name without the prefix /code)

repl
   start a Clojure repl with the active-paper classpath
   (This is a development tool.)

script
  run a Clojure script in the active-paper environment, usually to
  create an active-paper

swank-server
   start a swank server with the active-paper classpath
   (This is a development tool.)

update <active-paper> <dataset>=<value> ...
  updates the specified datasets and runs all calclets required
  to re-calculate dependent datasets
")

(def help-text-open
"Commands:

analyze <active-paper>
  show the dependencies between the datasets in <active-paper>

exit
  exit from the command interpreter

print <active-paper> <dataset>
  prints the contents of a dataset

rebuild <active-paper> <rebuilt-active-paper>
  copies all non-dependent items from <active-paper> to <rebuilt-active-paper>
  and runs the calclets to rebuild the dependent items

run_calclet <active-paper> <calclet-name>
  run the named calclet from the active-paper
  (provide the calclet name without the prefix /code)

update <active-paper> <dataset>=<value> ...
  updates the specified datasets and runs all calclets required
  to re-calculate dependent datasets
  A value can be an integer, a float, or a string, or an
  array (1d) of integers or an array (1d) of floats. Array elements are
  enclosed in square brackets and separated by commas. Spaces are not allowed
  inside array values!

<calclet-name>
  run the named calclet from the active-paper
")

(def open-commands
     {"analyze"     [analyze 1]
      "rebuild"     [rebuild 2]
      "run_calclet" [run-calclet 2]
      "update"      [update [1 nil]]
      "print"       [print-ds 2]})

(defn open
  [filename]
  (assert-file-exists filename)
  (let [paper (storage/open (File. filename) :read-write)]
    (letfn [(prompt-for-line []
              (printf "%s: " filename)
              (flush)
              (read-line))
            (next-line []
              (let [line (prompt-for-line)]
                (if (nil? line)
                  (throw (Exception.))
                  (str/split (str/trim line) #" +"))))]
      (try
        (loop []
          (let [input        (next-line)
                command-name (first input)
                args         (rest input)]
            (when (pos? (count command-name))
              (cond (or (= command-name "help") (= command-name "?"))
                    (println help-text-open)
                    (= command-name "exit")
                    (throw (Exception.))
                    :else
                    (let [[command nargs]     (open-commands command-name)
                          [min_args max_args] (if (number? nargs)
                                                [nargs nargs]
                                                nargs)]
                      (cond (nil? command)
                            (if (and (not (nil? (storage/get-code
                                                 paper command-name)))
                                     (zero? (count args)))
                              (run-calclet paper command-name)
                              (println help-text-open))
                            (or (and (number? min_args)
                                     (< (inc (count args)) min_args))
                                (and (number? max_args)
                                     (> (inc (count args)) max_args)))
                            (println (str command-name " needs " min_args
                                          " to " max_args  " arguments"))
                            :else
                            (try
                              (binding [run/*print-calclet-execution-trace*
                                        true]
                                (apply command (cons paper args)))
                              (catch Exception e
                                (when-not (= "error-exit" (.getMessage e))
                                  (throw e))))))))
            (recur)))
        (catch Exception e nil))
      (storage/close paper))))

(def commands
     {"repl"          [clojure.main/main 0]
      "swank-server"  [swank-server 0]
      "analyze"       [analyze 1]
      "rebuild"       [rebuild 2]
      "script"        [script 1]
      "run_calclet"   [run-calclet 2]
      "update"        [update [1 nil]]
      "make_library"  [make-library [1 nil]]
      "print"         [print-ds 2]
      "open"          [open 1]})

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
         (catch Exception e
           (when-not (= "error-exit" (.getMessage e))
             (throw e)))))))
