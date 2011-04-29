(ns e-paper.authoring
  (:require [e-paper.storage :as storage])
  (:require [e-paper.security :as security])
  (:require [e-paper.execution :as execution])
  (:import e_paper.ExecutablePaperRef)
  (:require [clojure.contrib.pprint :as pprint]))

(defn prepare-script
  ([paper]
   (prepare-script paper nil))
  ([paper name]
   (security/start-secure-mode)
   (let [cl (.getClassLoader ExecutablePaperRef)
         acc (:accessor paper)]
     (ExecutablePaperRef/setAccessors cl
       acc
       (if (isa? (class acc)
                 ch.systemsx.cisd.hdf5.IHDF5Writer)
         acc
         nil))
     (ExecutablePaperRef/setCurrentCalclet cl (str "/code/" name))
     (ExecutablePaperRef/initializeDependencyList cl))
   (when (not (nil? name))
     (remove-ns name))))

(defn cleanup-script
  []
  (let [cl (.getClassLoader ExecutablePaperRef)]
    (ExecutablePaperRef/clearDependencyList cl)
    (ExecutablePaperRef/setCurrentCalclet cl nil)
    (ExecutablePaperRef/setAccessors cl nil nil)))

(defmacro clojure-script
  [paper jars & body]
  (assert (= (first (first body)) 'ns))
  (let [name (second (first body))]
    `(do
       (storage/store-program ~paper (str '~name) ~jars "clojure.launcher"
             [{:type :text-file :contents (apply str (map prn-str '~body))}])
       (let [~'current-ns (ns-name *ns*)]
         (prepare-script ~paper '~name)
         (try
           (doseq [~'form '~body]
             (eval ~'form))
           (finally
            (cleanup-script)
            (in-ns ~'current-ns)))))))

(defn script
  [paper name script-engine jars text]
  (let [ds (storage/store-script paper name text script-engine jars)]
    (execution/run-calclet ds)))
