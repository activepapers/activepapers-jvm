(ns e-paper.dependencies
  (:require [clj-hdf5.core :as hdf5])
  (:require [e-paper.storage :as storage])
  (:import java.io.File))

(defn- datatype
  [node]
  (hdf5/get-attribute node "e-paper-datatype"))

(defn- items
  [paper]
  (hdf5/walk paper identity (fn [n] (nil? (datatype n)))))

(defn- e-paper-items
  [paper]
  (filter datatype (items paper)))

(defn non-e-paper-items
  [paper]
  (filter #(and (nil? (datatype %))
                (or (not (hdf5/group? %))
                    (empty? (hdf5/members %))))
          (items paper)))

(defn library-references
  [paper]
  (filter storage/reference? (e-paper-items paper)))

(defn library-dependencies
  [paper]
  (set (map #(first (hdf5/read %))
            (library-references paper))))

(defn primary-items
  [paper]
  (filter (fn [item]
            (and (not (storage/reference? item))
                 (let [deps (hdf5/get-attribute item "e-paper-dependencies")
                       deps (and deps (hdf5/read deps))
                       deps (filter #(pos? (count %)) deps)]
                   (empty? deps))))
          (e-paper-items paper)))
