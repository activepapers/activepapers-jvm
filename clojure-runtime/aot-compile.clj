(binding [*compile-path* (first *command-line-args*)]
  (compile 'e-paper-runtime.data))
