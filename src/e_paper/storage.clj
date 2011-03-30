(ns e-paper.storage
  (:require [e-paper.hdf5 :as hdf5])
  (:require [e-paper.utility :as utility]))

(defn create
  [filename]
  (let [root (hdf5/create filename)]
    (hdf5/create-group root "code")
    (hdf5/create-group root "data")
    (hdf5/create-group root "text")
    root))

(def open hdf5/open)

(def close hdf5/close)

(defn store-jar
  [paper ds-name jar-filename]
  (let [code (hdf5/lookup paper "code")]
    (hdf5/create-opaque-dataset
       code ds-name "jar" (utility/read-file jar-filename))))

(defn store-script
  [paper ds-name script-filename jars main-class-name args]
  (let [script (if (nil? script-filename) "" (slurp script-filename))
        args   (map (fn [a] (if (= a :script-filename) "" a)) args)
        ds (hdf5/create-string-dataset (hdf5/lookup paper "code") ds-name script)]
    (hdf5/write ds (into-array String [script]))
    (hdf5/create-string-attribute ds "jvm-main-class" [main-class-name])
    ; Add an empty string because HDF5 cannot handle empty arrays.
    (hdf5/create-string-attribute ds "jvm-args" (conj (vec args) ""))
    (hdf5/create-reference-attribute ds "jvm-jar-files" jars)
    ds))

(defn get-script
  [paper script-name]
  (hdf5/lookup (hdf5/lookup paper "code") script-name))

(defn- write-jar
  [tempfiles ds]
  (let [tf  (utility/create-tempfile "ep-" ".jar"
                                     (hdf5/read-opaque-data ds))]
    (cons tf tempfiles)))

(defn- write-script
  [ds]
  (utility/create-tempfile "ep-" ""
                           (hdf5/read-string-dataset ds)))

(defn run-script
  [script]
  (let [class-name   (first (hdf5/read-attribute script "jvm-main-class"))
        args         (into-array String
                                 (pop (hdf5/read-attribute script "jvm-args")))
        jars         (map #(hdf5/retrieve-object-from-ref
                            (hdf5/lookup script "/") %)
                          (hdf5/read-attribute script "jvm-jar-files"))
        jar-files    (reduce write-jar '() jars)
        script-file  (write-script script)
        temp-files   (cons script-file jar-files)
        args         (map (fn [a]
                            (if (empty? a)
                              (.getAbsolutePath script-file)
                              a))
                          args)]
    (try
      (let [loader      (new java.net.URLClassLoader
                             (into-array (map #(.toURL %) jar-files)))
            init-class  (.loadClass loader class-name)
            main        (.getDeclaredMethod init-class "main"
                           (into-array Class [(class (make-array String 0))]))]
        (. main invoke nil (into-array Object [(into-array String args)])))
      (finally
       (dorun (map #(.delete %) temp-files))))))
