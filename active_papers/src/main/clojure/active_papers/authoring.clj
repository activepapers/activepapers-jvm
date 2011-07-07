(ns active-paper.authoring
  (:require [active-paper.storage :as storage])
  (:require [active-paper.security :as security])
  (:require [active-paper.execution :as execution])
  (:import active_paper.ActivePaperRef)
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
