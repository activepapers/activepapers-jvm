(ns e-paper.utility)

(defn read-file
  "Read the contents of a file into a byte array"
  [file]
  (assert (isa? (class file) java.io.File))
  (let [size   (.  file length)
        stream (new java.io.FileInputStream file)
        buffer (byte-array size)]
    (. stream read buffer)
    buffer))

(defn create-tempfile
  "Create a temporary file."
  [prefix suffix data]
  (let [file   (java.io.File/createTempFile prefix suffix)]
    (if (string? data)
      (with-open [writer (clojure.java.io/writer file)]
        (.write writer data))
      (do
        (assert (isa? (class data) (class (byte-array 0))))
        (with-open [stream (clojure.java.io/output-stream file)]
          (.write stream data 0 (count data)))))
    file))

