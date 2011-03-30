(ns e-paper.utility)

(defn read-file
  "Read the contents of a file into a byte array"
  [filename]
  (let [file   (new java.io.File filename)
        size   (.  file length)
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


;; (defn create-tempfile
;;   "Create a temporary file."
;;   [prefix suffix bytes]
;;   (let [file   (java.io.File/createTempFile prefix suffix)]
;;     (with-open [stream (clojure.java.io/output-stream file)]
;;       (.write stream bytes 0 (count bytes)))
;;     file))

;; (defn create-tempfile
;;   "Create a temporary file."
;;   [prefix suffix string]
;;   (let [file   (java.io.File/createTempFile prefix suffix)]
    
;;     file))
