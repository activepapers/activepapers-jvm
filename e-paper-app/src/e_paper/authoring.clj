(ns e-paper.authoring
  (:require [e-paper.storage :as storage])
  (:require [e-paper.security :as security])
  (:require [e-paper.execution :as execution])
  (:import e_paper.ExecutablePaperRef)
  (:require [clojure.contrib.pprint :as pprint]))

(defmacro clojure-script
  [paper jars & body]
  (assert (= (first (first body)) 'ns))
  (let [name (second (first body))]
    `(storage/store-program
      ~paper (str '~name) ~jars "clojure.launcher"
      [{:type :text-file
        :contents (apply str (map prn-str '~body))}])))

(defn script
  [paper name script-engine jars text]
  (storage/store-script paper name text script-engine jars))
