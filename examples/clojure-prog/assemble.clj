(ns clojure-prog.assemble
  (:require [e-paper.storage :as ep])
  (:require [e-paper.authoring :as auth])
  (:import java.io.File))

(def dir (File. "/Users/hinsen/projects/e-paper/examples/clojure-prog/"))

(def paper (ep/create (File. dir "clojure_paper.h5")))

(def jars (ep/store-library-references paper "clojure"))

(auth/script paper jars
  (ns generate-input
    (:require [e-paper-runtime.data :as data]))
  (data/create-dataset "time" (vec (range 0. 10. 0.1)))
  (data/create-dataset "frequency" 0.2))

(auth/script paper jars
  (ns calc-sine
    (:require [e-paper-runtime.data :as data])
    (:require [clj-hdf5.core :as hdf5]))
  (let [time      (hdf5/read (data/get-dataset "time"))
        frequency (hdf5/read (data/get-dataset "frequency"))
        sine      (map #(Math/sin (* 2 Math/PI frequency %)) time)]
    (data/create-dataset "sine" sine)))

(comment

  (ep/store-program paper "generate-input"
                    jars "clojure.launcher"
                    [{:type :text-file
                      :contents (slurp
                                 (File. dir "code/generate_input.clj"))}])

  (ep/run-program (ep/get-code paper "generate-input"))

  (ep/store-program paper "calc-sine"
                    jars "clojure.launcher"
                    [{:type :text-file
                      :contents (slurp
                                 (File. dir "code/calc_sine.clj"))}])

  (ep/run-program (ep/get-code paper "calc-sine")))

(ep/close paper)
