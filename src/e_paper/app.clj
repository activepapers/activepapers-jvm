(ns e-paper.app
  (:gen-class)
  (:require clojure.main)
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.storage :as storage])
  (:require [e-paper.dependencies :as deps])
  (:require [e-paper.execution :as run])
  (:import java.io.File))

(defn check-file
  [filename]
  (when-not [(.exists (File. filename))]
    (.println System/err (str "File " filename " not found"))
    (throw (Exception.))))

(defn rebuild
  [in-filename out-filename]
  (check-file in-filename)
  (run/rebuild-from-primary-items (storage/open (File. in-filename))
                                  (File. out-filename)))

(defn analyze
  [filename]
  (check-file filename)
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
      "script"  [clojure.main/main 1]})

(def help-text
"Commands:

analyze <e-paper>
  show the dependencies between the datasets in <e-paper>

rebuild <e-paper> <rebuilt-e-paper>
  copies all non-dependent items from <e-paper> to <rebuilt-e-paper>
  and runs the calclets to rebuild the dependent items

repl
   start a Clojure repl with the e-paper classpath

script
  run a Clojure script in the e-paper environment, usually to
  create an e-paper
")

(defn -main [& args]
  (let [[command-name & args] args
        [command nargs]       (commands command-name)]
    (if (or (nil? command)
            (not= (count args) nargs))
      (println help-text)
      (try
        (apply command args)
        (catch Exception e nil)))))
