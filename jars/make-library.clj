(ns e-paper.make-library
  (:require [e-paper.storage :as ep])
  (:import java.io.File))

(def cwd (try (File. (deref (ns-resolve 'cake '*pwd*)))
              (catch Exception e nil)))

(defn parse-jar-spec
  [jar-spec]
  (let [[_ ds-name jar-file]
        (re-matches #"([a-zA-Z0-9-_]+)=(.*\.jar)" jar-spec)]
    (if (nil? ds-name)
      (do (println (str "invalid argument: " jar-spec))
          (System/exit 1))
      [ds-name jar-file])))

(let [[library-file & jar-specs] *command-line-args*
      jar-specs (map parse-jar-spec jar-specs)
      library (ep/create (File. cwd library-file))]
  (doseq [[ds-name jar-file] jar-specs]
    (ep/store-jar library ds-name (File. cwd jar-file)))
  (ep/close library))
