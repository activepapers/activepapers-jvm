(ns e-paper.jhdf5
  (:refer-clojure :exclude [read]))

; A node is defined by its file and its path inside that file

(defrecord hdf-node
  [accessor path])

(defrecord hdf-attribute
  [accessor path attrname])

(defn- path-concat
  [abs-path rel-path]
  (if (= abs-path "/")
    (str "/" rel-path)
    (str abs-path "/" rel-path)))

; Type checks

(defn node?
  [object]
  (isa? (class object) hdf-node))

(defn group?
  [object]
  (and (node? object)
       (. (:accessor object)  isGroup (:path object))))

(defn dataset?
  [object]
  (and (node? object)
       (. (:accessor object)  isDataSet (:path object))))

(defn root?
  [object]
  (and (group? object)
       (= (:path object) "/")))

(defn attribute?
  [object]
  (isa? (class object) hdf-attribute))

; Opening and closing files.
; The return value of open/create is the root group object.

(defn open
  ([filename] (open filename :read-only))
  ([filename mode]
     (let [factory (ch.systemsx.cisd.hdf5.HDF5FactoryProvider/get)
           file (new java.io.File filename)]
       (new hdf-node
            (case mode
                  :read-only   (. factory openForReading file)
                  :read-write  (. factory open file)
                  :create      (let [configurator (. factory configure file)]
                                 (. configurator overwrite)
                                 (. configurator writer)))
            "/"))))

(defn create
  [filename]
  (open filename :create))

(defn close
  [root-group]
  (assert (root? root-group))
  (. (:accessor root-group) close))

; Datatypes

(defn datatype
  [object]
  (assert (or (dataset? object)
              (attribute? object)))
  (let [acc  (:accessor object)
        path (:path object)]
    (if (dataset? object)
      (. (. acc getDataSetInformation path) getTypeInformation)
      (. acc getAttributeInformation path (:attrname object)))))

; Reading datasets and attributes

(defmulti read class)

; Nodes

(defn file
  [node]
  (assert (node? node))
  (. (:accessor node) getFile))

(defn path
  [node]
  (assert (node? node))
  (:path node))

(defn parent
  [node]
  (assert (node? node))
  (let [path (clojure.string/split (:path node) #"/")]
    (if (empty? path)
      nil
      (let [parent-path (subvec path 0 (dec (count path)))]
        (new hdf-node
             (:accessor node)
             (if (= (count parent-path) 1)
               "/"
               (clojure.string/join "/" parent-path)))))))

(defn attributes
  [node]
  (assert (node? node))
  (let [acc   (:accessor node)
        path  (:path node)
        names (. acc  getAttributeNames path)]
    (into {} (for [n names]
               [n (new hdf-attribute acc path n)]))))

(defn lookup-attribute
  [node name]
  (assert (node? node))
  (let [acc  (:accessor node)
        path (:path node)]
    (if (. acc hasAttribute path name)
      (new hdf-attribute acc path name)
      nil)))

(defmethod read hdf-attribute
  [attr]
  (let [acc    (:accessor attr)
        path   (:path attr)
        name   (:attrname attr)
        dt     (datatype attr)
        dclass (. dt getDataClass)
        ddims  (vec (. dt getDimensions))]
    (cond
       (= dclass ch.systemsx.cisd.hdf5.HDF5DataClass/STRING)
          (. acc getStringAttribute path name)
       (= dclass ch.systemsx.cisd.hdf5.HDF5DataClass/INTEGER)
          (. acc getLongAttribute path name)
       (= dclass ch.systemsx.cisd.hdf5.HDF5DataClass/FLOAT)
          (. acc getDoubleAttribute path name)
       :else
          nil)))

; Groups

(defn children
  [group]
  (assert (group? group))
  (into {}
        (for [name (. (:accessor group) getAllGroupMembers (:path group))]
          [name (new hdf-node
                     (:accessor group)
                     (path-concat (:path group) name))])))

(defn lookup
  [group name]
  (assert (group? group))
  (let [acc       (:accessor group)
        path      (:path group)
        full-path (path-concat path name)]
    (if (. acc exists full-path)
      (new hdf-node acc full-path)
      nil)))

(defn create-group
  [parent name]
  (assert (group? parent))
  (. (:accessor parent) createGroup name)
  (lookup parent name))

; Datasets

(defn dimensions
  [dataset]
  (assert (dataset? dataset))
  (vec (. (. (:accessor dataset) getDataSetInformation (:path dataset))
          getDimensions)))

(defn max-dimensions
  [dataset]
  (assert (dataset? dataset))
  (vec (. (. (:accessor dataset) getDataSetInformation (:path dataset))
          getMaxDimensions)))

(defn rank
  [dataset]
  (assert (dataset? dataset))
  (. (. (:accessor dataset) getDataSetInformation (:path dataset))
     getRank))

(defmulti create-dataset
  (fn [parent name data] (type data)))

(defmethod create-dataset java.lang.String
  [parent name data]
  (. (:accessor parent) writeString
     (path-concat (:path parent) name)
     data
     (inc (count data))))

;(defmethod read hdf-node
;  [dataset]
;  (assert (dataset? dataset))
;  (. (:accessor dataset) getDataSetInformation (:path dataset)))

; Special-case treatment for opaque data.
; The HDF Object layer doesn't handle opaque data, so this needs
; to be done in low-level code.
(defn create-opaque-dataset
  [parent name tag data]
  )

(defn opaque-tag
  "Return the tag of an opaque dataset."
  [opaque-ds]
  )

(defn read-opaque-data
  "Read an opaque dataset into a bytestream."
  [opaque-ds]
  )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(def hfile (open "/Users/hinsen/projects/sputnik/data_model/examples/test.h5"))
(def conf (lookup hfile "conf"))

(read (lookup-attribute conf "DATA_MODEL"))

(close hfile)

