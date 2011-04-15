(ns e-paper.authoring
  (:require [e-paper.storage :as storage])
  (:import e_paper.ExecutablePaperRef)
  (:require [clojure.contrib.pprint :as pprint]))

(defn prepare-script
  ([paper]
   (prepare-script paper nil))
  ([paper name]
   (let [acc (:accessor paper)]
     (ExecutablePaperRef/setAccessors
      acc
      (if (isa? (class acc)
                ch.systemsx.cisd.hdf5.IHDF5Writer)
        acc
        nil)))
   (ExecutablePaperRef/setCurrentProgram (str "/code/" name))
   (ExecutablePaperRef/initializeDependencyList)
   (when (not (nil? name))
     (remove-ns name))))

(defn cleanup-script
  []
  (ExecutablePaperRef/clearDependencyList)
  (ExecutablePaperRef/setCurrentProgram nil)
  (ExecutablePaperRef/setAccessors nil nil))

(defmacro script
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
