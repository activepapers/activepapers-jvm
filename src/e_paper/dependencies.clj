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

(defn e-paper-items
  "Find all items in the paper that are recognized by the
   e-paper infrastructure."
  [paper]
  (filter datatype (items paper)))

(defn non-e-paper-items
  "Find all items in the paper that are ignored by the e-paper infrastructure."
  [paper]
  (filter #(and (nil? (datatype %))
                (or (not (hdf5/group? %))
                    (empty? (hdf5/members %))))
          (items paper)))

(defn library-references
  "Find all references to library data in the paper."
  [paper]
  (filter storage/reference? (e-paper-items paper)))

(defn library-dependencies
  "Return a map from library names to vectors of references that point
   to that library."
  [paper]
  (reduce (fn [deps new-ref]
            (let [library (first (hdf5/read new-ref))]
              (assoc deps library
                     (conj (get deps library []) new-ref))))
          {}
          (library-references paper)))

(defn dependencies
  "Find the dependencies of item. Return nil if there are none."
  [item]
  (let [; look up the attribute storing dependencies
        deps (hdf5/get-attribute item "e-paper-dependencies")
        ; if the attribute exists, read it
        deps (and deps (hdf5/read deps))
        ; remove the empty string that is added to keep HDF5 happy
        deps (filter #(pos? (count %)) deps)]
    (map #(hdf5/get-dataset (hdf5/root item) %) deps)))

(defn primary-items
  "Find the primary items in the paper, i.e. the items that have no
   dependencies on other items."
  [paper]
  (filter (comp empty? dependencies)
          (e-paper-items paper)))

(defn dependent-items
  "Find all items in the paper that have dependencies."
  [paper]
  (filter dependencies (e-paper-items paper)))

(defn dependency-hierarchy
  "Return a sequence of sets of items in the paper such that the items
   in any sets depend only on items in the previous sets. The first set
   contains the dependency-free items."
  [paper]
  (let [items   (map #(vector % (set (dependencies %))) (e-paper-items paper))
        groups  (group-by (comp empty? second) items)
        known   (set (map first (get groups true)))
        unknown (get groups false)
        levels  [known]]
    (loop [levels  levels
           known   known
           unknown unknown]
      (if (empty? unknown)
        levels
        (let [next    (set (map first
                                (filter (fn [[_ deps]]
                                          (clojure.set/subset? deps known))
                                        unknown)))
              known   (clojure.set/union known next)
              unknown (filter (fn [[item _]] (not (contains? next item)))
                              unknown)]
          (when (empty? next)
            (throw (Exception. "cyclic dependencies")))
          (recur (conj levels next) known unknown))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;(def paper (storage/open (java.io.File.  "/Users/hinsen/projects/e-paper/examples/clojure-prog/clojure_paper.h5")))