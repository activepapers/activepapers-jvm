(ns active-papers.authoring
  (:require [active-papers.storage :as storage])
  (:require [active-papers.security :as security])
  (:require [active-papers.execution :as execution])
  (:import active_papers.ActivePaperRef)
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
