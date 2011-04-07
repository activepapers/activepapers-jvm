(ns e-paper.security)

(def *access-control* true)

(def security-manager
     (proxy [java.lang.SecurityManager] []
     (checkPermission
      ([permission]
         (when *access-control*
           (java.security.AccessController/checkPermission permission)))
      ([permission context]
         (when *access-control*
           (if (isa? (class context) java.security.AccessControlContext)
             (. context checkPermission permission)
             (throw (SecurityException.))))))))

(defn start-secure-mode
  []
  (let [sm (System/getSecurityManager)]
    (cond (nil? sm) (System/setSecurityManager security-manager)
          (identical? sm security-manager) nil
          :else (throw (Exception.
                        "a different security manager is already in place")))))

(defmacro with-full-permissions
  [& body]
  `(do (start-secure-mode)
       (binding [*access-control* false]
         ~@body)))

(defmacro with-restricted-permissions
  [& body]
  `(do (start-secure-mode)
       (binding [*access-control* true]
         ~@body)))
