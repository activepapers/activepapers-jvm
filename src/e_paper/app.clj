(ns e-paper.app
  (:gen-class)
  (:require clojure.main))

(def commands
     {"repl" [clojure.main/main 0]})

(def help-text
"Commands:

repl: start a Clojure repl with the e-paper classpath
")

(defn -main [& args]
  (let [[command-name & args] args
        [command nargs]       (commands command-name)]
    (if (or (nil? command)
            (not= (count args) nargs))
      (println help-text)
      (apply command args))))
