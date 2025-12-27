(ns build
  "The build script for the example of the poly documentation.

   Targets:
   * uberjar :project PROJECT
     - creates an uberjar for the given project

   For help, run:
     clojure -A:deps -T:build help/doc

   Create uberjar for command-line:
     clojure -T:build uberjar :project command-line"
  (:require
   [clojure.java.io :as io]
   [clojure.tools.build.api :as b]
   [clojure.tools.deps :as t]
   [clojure.tools.deps.util.dir :refer [with-dir]]))

(defn- get-project-aliases 
  "判断当前这个 dep.edn 有哪些 alias"
  []
  (let [edn-fn (juxt :root-edn :project-edn)]
    (-> (t/find-edn-maps)
        (edn-fn)
        (t/merge-edns)
        :aliases)))

(comment 
  (get-project-aliases) ;; 当前目录的 deps.edn

  ;; 切换目录之后看 alias
  (with-dir (io/file "...") 
    (get-project-aliases))
  :rcf)

(defn- ensure-project-root
  "Given a task name and a project name, ensure the project
   exists and seems valid, and return the absolute path to it."
  [task project]
  (let [project-root (str (System/getProperty "user.dir") "/projects/" project)]
    (when-not (and project
                   (.exists (io/file project-root))
                   (.exists (io/file (str project-root "/deps.edn"))))
      (throw (ex-info (str task " task requires a valid :project option") {:project project})))
    project-root))

(comment
  ;; task 只是用来打日志的, 和 project 无关
  (ensure-project-root "uberjar" "web-app")
  :rcf)

(defn uberjar
  "Builds an uberjar for the specified project.

   Options:
   * :project - required, the name of the project to build,
   * :uber-file - optional, the path of the JAR file to build,
     relative to the project folder; can also be specified in
     the :uberjar alias in the project's deps.edn file; will
     default to target/PROJECT.jar if not specified.

   Returns:
   * the input opts with :class-dir, :compile-opts, :main, and :uber-file
     computed.

   The project's deps.edn file must contain an :uberjar alias
   which must contain at least :main, specifying the main ns
   (to compile and to invoke)."
  [{:keys [project uber-file] :as opts}]
  (let [project-root (ensure-project-root "uberjar" project)
        aliases      (with-dir (io/file project-root) (get-project-aliases))
        main         (-> aliases :uberjar :main)] 
    (when-not main
      (throw (ex-info (str "the " project " project's deps.edn file does not specify the :main namespace in its :uberjar alias")
                      {:aliases aliases})))
    
    (b/with-project-root project-root
      (let [class-dir "target/classes"
            uber-file (or uber-file
                          (-> aliases :uberjar :uber-file)
                          (str "target/" project ".jar"))
            opts      (merge opts
                             {:basis        (b/create-basis)
                              :class-dir    class-dir
                              :compile-opts {:direct-linking true}
                              :main         main
                              :ns-compile   [main]
                              :uber-file    uber-file})] 
        
        (b/delete {:path class-dir})
        (println "\nCompiling" (str main "..."))
        
        (try
          (b/compile-clj opts)
          (catch Exception e
            (println "\nCompilation Error:")
            (println (.getMessage e))
            (println "\nStack Trace:")
            (.printStackTrace e)
            (throw e)))
        
        (println "Building uberjar" (str uber-file "..."))
        (b/uber opts)
        (println "clearning uberjar build files")
        (b/delete {:path class-dir})
        (println "Uberjar is built.")
        opts))))

(comment 
  (uberjar {:project "web-app"})
  :rcf)
