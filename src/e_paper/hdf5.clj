(ns e-paper.hdf5
  (:require clojure.string))

; Opening files. The return value is the root group object.

(defn open
  ([filename] (open filename :read-only))
  ([filename mode]
     (let [mode (get {:read-only  ncsa.hdf.object.h5.H5File/READ
                      :read-write ncsa.hdf.object.h5.H5File/WRITE
                      :create     ncsa.hdf.object.h5.H5File/CREATE}
                     mode)
           file (ncsa.hdf.object.h5.H5File. filename mode)]
       (. file open)
       (. (. file getRootNode) getUserObject))))

(defn create
  [filename]
  (open filename :create))

; Type checks

(defn hdf-object?
  [object]
  (isa? (class object) ncsa.hdf.object.HObject))

(defn group?
  [object]
  (isa? (class object) ncsa.hdf.object.Group))

(defn dataset?
  [object]
  (isa? (class object) ncsa.hdf.object.Dataset))

(defn node?
  [object]
  (or (group? object) (dataset? object)))

(defn root?
  [object]
  (and (group? object) (.isRoot object)))

(defn attribute?
  [object]
  (isa? (class object) ncsa.hdf.object.Attribute))

; Nodes

(defn file
  [node]
  (assert (node? node))
  (. node getFileFormat))

(defn close
  [root-group]
  (assert (root? root-group))
  (. (file root-group) close))

(defn path
  [node]
  (assert (node? node))
  (if (root? node)
    "/"
    (. node getFullName)))

(defn parent
  [node]
  (assert (node? node))
  (if (root? node)
    nil
    (. node getParent)))

(defn metadata
  [object]
  (assert (hdf-object? object))
  (vec (. object getMetadata)))

; Groups

(defn children
  [group]
  (assert (group? group))
  (into {} (map (fn [x] [(. x getName) x]) (. group getMemberList))))

(defn get-child
  [group name]
  (assert (group? group))
  (loop [children (. group getMemberList)]
    (if (empty? children)
      nil
      (let [c (first children)]
        (if (= name (. c getName))
          c
          (recur (rest children)))))))

(defn lookup
  [group path]
  (let [from-root (= (get path 0) \/)
        path      (filter #(pos? (count %)) (clojure.string/split path #"/"))
        group     (if from-root
                    (. (. (file group) getRootNode) getUserObject)
                    group)]
    (loop [g group
           p path]
      (cond
       (empty? p) g
       (nil? g)   nil
       :else (recur (get-child  g (first p)) (rest p))))))

(defn create-group
  [parent name]
  (assert (group? parent))
  (. (file parent) createGroup name parent))

; Datatypes

(defn datatype?
  [object]
  (isa? (class object) ncsa.hdf.object.Datatype))

(defn datatype
  [dataset-or-attribute]
  (assert (or (dataset? dataset-or-attribute)
              (attribute? dataset-or-attribute)))
  (if (dataset? dataset-or-attribute)
    (. dataset-or-attribute getDatatype)
    (. dataset-or-attribute getType)))

(defmethod print-method ncsa.hdf.object.Datatype
  [dt ^java.io.Writer w]
  (.write w "#<H5Datatype \"")
  (.write w (. dt getDatatypeDescription))
  (.write w "\">"))

(def ^{:private true} datatype-classes
     {:array ncsa.hdf.object.Datatype/CLASS_ARRAY
      :bitfield ncsa.hdf.object.Datatype/CLASS_BITFIELD
      :char ncsa.hdf.object.Datatype/CLASS_CHAR
      :compound ncsa.hdf.object.Datatype/CLASS_COMPOUND
      :enum ncsa.hdf.object.Datatype/CLASS_ENUM
      :float ncsa.hdf.object.Datatype/CLASS_FLOAT
      :integer ncsa.hdf.object.Datatype/CLASS_INTEGER
      :no-class ncsa.hdf.object.Datatype/CLASS_NO_CLASS
    ; :opaque ncsa.hdf.object.Datatype/CLASS_OPAQUE  (not supported by HDF Object)
      :reference ncsa.hdf.object.Datatype/CLASS_REFERENCE
      :string ncsa.hdf.object.Datatype/CLASS_STRING
      :vlen ncsa.hdf.object.Datatype/CLASS_VLEN})

(def ^{:private true} datatype-sizes
     {:native ncsa.hdf.object.Datatype/NATIVE})

(def ^{:private true} datatype-orders
     {:native ncsa.hdf.object.Datatype/NATIVE
      :big-endian ncsa.hdf.object.Datatype/ORDER_BE
      :little-endian ncsa.hdf.object.Datatype/ORDER_LE
      :none ncsa.hdf.object.Datatype/ORDER_NONE
      :vax ncsa.hdf.object.Datatype/ORDER_VAX})

(def ^{:private true} datatype-signs
     {:native ncsa.hdf.object.Datatype/NATIVE
      :nsgn ncsa.hdf.object.Datatype/NSGN
      :sign-2 ncsa.hdf.object.Datatype/SIGN_2
      :none ncsa.hdf.object.Datatype/SIGN_NONE})

(defn- kwarg
  [dict value]
  (if (keyword? value)
    (get dict value)
    value))

(defn make-datatype
  [tclass tsize torder tsign]
  (new ncsa.hdf.object.h5.H5Datatype
     (kwarg datatype-classes tclass)
     (kwarg datatype-sizes tsize)
     (kwarg datatype-orders torder)
     (kwarg datatype-signs tsign)))

(defn create-named-datatype
  [root-group name tclass tsize torder tsign]
  (assert (root? root-group))
  (. (file root-group) createDatatype
     (kwarg datatype-classes tclass)
     (kwarg datatype-sizes tsize)
     (kwarg datatype-orders torder)
     (kwarg datatype-signs tsign)
     name))

; Datasets

(defn create-dataset
  ([parent name datatype dims max-dims chunks gzip-level]
   (assert (group? parent))
   (. (file parent) createScalarDS
      name parent datatype
      (if (nil? dims) nil (long-array dims))
      (if (nil? max-dims) nil (long-array max-dims))
      (if (nil? chunks) nil (long-array chunks))
      gzip-level nil))
  ([parent name datatype dims max-dims chunks]
     (create-dataset parent name datatype dims max-dims chunks 0))
  ([parent name datatype dims max-dims]
     (create-dataset parent name datatype dims max-dims nil 0))
  ([parent name datatype dims]
     (create-dataset parent name datatype dims nil nil 0)))

(defn write
  [ds data]
  (. ds write data))

(defn read
  [ds]
  (.getData ds))

(defn create-string-dataset
  [parent name string]
  (let [ds (create-dataset parent name
                           (make-datatype :string (inc (count string))
                                          :native :native)
                           [1])]
    (write ds (into-array [string]))
    ds))

(defn read-string-dataset
  [ds]
  (let [size (.getDatatypeSize (datatype ds))]
    (first (ncsa.hdf.object.Dataset/byteToString (.readBytes ds) size))))

; Special-case treatment for opaque data.
; The HDF Object layer doesn't handle opaque data, so this needs
; to be done in low-level code.
(defn create-opaque-dataset
  [parent name tag data]
  (assert (group? parent))
  (assert (string? name))
  (assert (string? tag))
  (let [full-name (str (path parent) "/"  name)
        size      (count data)
        dims      (long-array [size])
        fid       (. parent getFID)
        tid       (. ncsa.hdf.hdf5lib.H5 H5Tcreate
                     ncsa.hdf.hdf5lib.HDF5Constants/H5T_OPAQUE 1)
        sid       (. ncsa.hdf.hdf5lib.H5 H5Screate_simple 1 dims nil)]
    (. ncsa.hdf.hdf5lib.H5 H5Tset_tag tid tag)
    (let [did  (. ncsa.hdf.hdf5lib.H5 H5Dcreate fid full-name tid sid
                  ncsa.hdf.hdf5lib.HDF5Constants/H5P_DEFAULT)]
      (. ncsa.hdf.hdf5lib.H5 H5Dwrite did tid
         ncsa.hdf.hdf5lib.HDF5Constants/H5S_ALL
         ncsa.hdf.hdf5lib.HDF5Constants/H5S_ALL
         ncsa.hdf.hdf5lib.HDF5Constants/H5P_DEFAULT
         data)
      (. ncsa.hdf.hdf5lib.H5 H5Dclose did)
      (. ncsa.hdf.hdf5lib.H5 H5Sclose sid)
      (. ncsa.hdf.hdf5lib.H5 H5Tclose tid)))
  (let [ds (new ncsa.hdf.object.h5.H5ScalarDS (file parent) name (path parent))]
    (. parent addToMemberList ds)
    ds))

(defn opaque-tag
  "Return the tag of an opaque dataset."
  [opaque-ds]
  (assert (dataset? opaque-ds))
  (let [did (. opaque-ds open)
        tid (. ncsa.hdf.hdf5lib.H5 H5Dget_type did)
        tag (. ncsa.hdf.hdf5lib.H5 H5Tget_tag tid)]
    (. ncsa.hdf.hdf5lib.H5 H5Tclose tid)
    (. opaque-ds close did)
    tag))

(defn read-opaque-data
  "Read an opaque dataset into a bytestream."
  [opaque-ds]
  (assert (dataset? opaque-ds))
  (. opaque-ds readBytes))

; Attributes

(defn attributes
  [node]
  (into {}
        (map (fn [x] [(. x getName) x])
             (filter #(attribute? %) (metadata node)))))

(defn get-attribute
  [node attrname]
  (first (filter #(and (attribute? %) (= (. % getName) attrname)) (metadata node))))

(defn read-attribute
  ([attribute]
   (assert (attribute? attribute))
   (vec (. attribute getValue)))
  ([node attrname]
   (read-attribute (get-attribute node attrname))))

(defn create-attribute
  [node name datatype dims value]
  (assert (node? node))
  (assert (string? name))
  (let [attr (new ncsa.hdf.object.Attribute name
                  datatype (long-array dims) value)]
    (. node writeMetadata attr)
    attr))

(defn- strs-to-byte-array
  [strings size]
  (. ncsa.hdf.object.Dataset stringToByte (into-array String strings) size))

(defn create-string-attribute
  [node name strings]
  (assert (every? string? strings))
  (let [maxlen (inc (reduce max (conj (map count strings) 0)))
        bytes  (strs-to-byte-array strings maxlen)]
    (create-attribute node name
                      (make-datatype :string maxlen :native :native)
                      [(count strings)] bytes)))

(defn create-int-attribute
  [node name ints]
  (assert (every? #(isa? (class %) Integer) ints))
  (create-attribute node name
                    (make-datatype :integer 4 :native :native)
                    [(count ints)] (into-array Integer/TYPE ints)))

(defn get-object-reference
  [object]
  (assert (node? object))
  (. ncsa.hdf.hdf5lib.H5 H5Rcreate
     (.getFID object)
     (path object)
     ncsa.hdf.hdf5lib.HDF5Constants/H5R_OBJECT
     -1))

(defn create-reference-attribute
  [node name objects]
  (assert (every? node? objects))
  (create-attribute node name
                    (make-datatype :reference :native :native :native)
                    [(count objects)]
                    (into-array (class (make-array Byte/TYPE 0))
                                (map get-object-reference objects))))

(defn retrieve-object-from-ref
  [root-group object-ref]
  (assert (root? root-group))
  (let [name-array (make-array String 1)]
    (loop [name-size 32]
      (let [real-size
            (. ncsa.hdf.hdf5lib.H5 H5Rget_name
               (.getFID root-group)
               ncsa.hdf.hdf5lib.HDF5Constants/H5R_OBJECT
               (. ncsa.hdf.hdf5lib.HDFNativeData longToByte object-ref)
               name-array name-size)]
        (when (> real-size name-size)
          (recur (inc real-size)))))
    (lookup root-group (first name-array))))

    