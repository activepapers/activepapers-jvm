(ns clojure-script.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.authoring :as auth])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-script/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def jars (ep/store-library-references paper "clojure"))

(auth/script paper "generate-input" "Clojure" jars
" (ns generate-input
    (:require [e-paper-runtime.data :as data]))
  (data/create-data \"time\" (vec (range 0. 10. 0.1)))
  (data/create-data \"frequency\" 0.2)
")
 
(auth/script paper "calc-sine" "Clojure" jars
" (ns calc-sine
    (:require [e-paper-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
  (let [time      (hdf5/read (data/get-data \"time\"))
        frequency (hdf5/read (data/get-data \"frequency\"))
        sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
    (data/create-data \"sine\" sine))
")

(ep/close paper)
