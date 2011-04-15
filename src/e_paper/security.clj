(ns e-paper.security
  (:import java.io.File))

; 
; When this var is true, the current Java policy is enforced by the
; security manager. When it is false, the security manager is deactivated,
; i.e. the permissions are those of a standard application.
; 
; Since the root binding is true, only code from the e-paper launcher
; can bind it to false, and it does so only through the macro
; with-full-permissions.
; 
(def *access-control* true)

;
; A SecurityManager that is active only when *access-control* is true.
;
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

;
; Handle security settings
;
(defn start-secure-mode
  "Install the security manager."
  []
  (let [sm (System/getSecurityManager)]
    (cond (nil? sm) (System/setSecurityManager security-manager)
          (identical? sm security-manager) nil
          :else (throw (Exception.
                        "a different security manager is already in place")))))

(defmacro with-full-permissions
  "Execute body will the permissions of a standard Java application."
  [& body]
  `(do (start-secure-mode)
       (binding [*access-control* false]
         ~@body)))

(defmacro with-restricted-permissions
  "Execute body with the permissions granted by the active policy."
  [& body]
  `(do (start-secure-mode)
       (binding [*access-control* true]
         ~@body)))

;
; A ClassLoader that isolates programs inside papers from the launcher.
; A few packages (the HDF5 library and the data-passing interface)
; are shared between the launcher and all programs in a paper.
; This makes it possible to grant in-paper code access only to specific
; HDF5 files: its own, and library files in read-only access.
;
(defn- starts-with
  [string prefix]
  (let [c (count prefix)]
    (and (>= (count string) c)
         (= prefix (subs string 0 c)))))

(defn make-class-loader
  [jar-files]
  (let [app-cl    (.getClassLoader clojure.lang.RT)]
    (proxy [java.net.URLClassLoader]
           [(into-array (map #(.toURL %) jar-files))
            (.getParent app-cl)]
      (findClass
       [name]
       (if (some (partial starts-with name)
                 ["ch.systemsx.cisd.hdf5."
                  "ncsa.hdf."
                  "e_paper.ExecutablePaperRef"])
         (.loadClass app-cl name)
         (proxy-super findClass name))))))

