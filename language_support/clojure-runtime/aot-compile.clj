(binding [*compile-path* (first *command-line-args*)]
  (compile 'active-paper-runtime.data))
