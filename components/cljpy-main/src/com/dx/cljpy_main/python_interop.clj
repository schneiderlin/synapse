(ns com.dx.cljpy-main.python-interop
  (:require 
   [clojure.string :as str]
   [libpython-clj2.python :as py]))

(defn get-os []
  (let [os-name (System/getProperty "os.name")]
    (cond
      (.contains os-name "Windows") :windows
      (.contains os-name "Mac") :mac
      :else :linux)))

(comment
  (get-os)
  (System/getProperty "user.name")
  :rcf)


(defn read-cfg-file
  "Read the .venv/pyvenv.cfg file and return it as EDN" []
  (let [cfg-path ".venv/pyvenv.cfg"
        cfg-content (slurp cfg-path)
        lines (str/split-lines cfg-content)]
    (into {}
          (for [line lines
                :when (and (seq line) (not (str/starts-with? line "#")))]
            (let [[key value] (str/split line #"\s*=\s*" 2)]
              [(keyword key) value])))))

(comment
  (read-cfg-file)
  :rcf)

(defn make-python-init-config [user-lib-paths]
  (let [cfg (read-cfg-file)
        version (-> (:version_info cfg)
                    (str/split #"\.")
                    (->> (take 2) (str/join "")))
        lib-name (str "python" version ".dll")]
    (case (get-os)
      :windows {:python-executable "./.venv/Scripts/python.exe"
                :library-path (str (:home cfg) "\\" lib-name)
                :user-lib-paths (conj user-lib-paths "./.venv/Lib/site-packages")}
      :linux {:python-executable "./.venv/bin/python"
              :user-lib-paths (conj user-lib-paths "./.venv/lib/python3.12/site-packages")})))

(defn make-python-env
  "user-lib-paths: list of user lib paths 
   modules: map, k: returned key, v: python module name
   
   return: key to python module"
  [user-lib-paths modules]
  (let [init-config (make-python-init-config user-lib-paths)]
    (py/initialize! init-config)
    (let [sys (py/import-module "sys")]
      (py/call-attr (py/get-attr sys "path") "extend" (:user-lib-paths init-config))
      #_(setup-python-debugger)

      (into {}
            (for [[k v] modules]
              [k (py/import-module v)])))))



