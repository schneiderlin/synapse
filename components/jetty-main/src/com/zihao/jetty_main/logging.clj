(ns com.zihao.jetty-main.logging
  (:require [clojure.java.io :as io]))

(defn ensure-logs-dir []
  (let [logs-dir (io/file "./public/log")
        public-dir (io/file "./public")]
    (println "Current working directory:" (System/getProperty "user.dir"))
    (when-not (.exists public-dir)
      (println "Creating public directory...")
      (.mkdirs public-dir))
    (when-not (.exists logs-dir)
      (println "Creating logs directory...")
      (.mkdirs logs-dir))))

(comment
  (ensure-logs-dir)
  :rcf)

(defn write-to-file [udid data]
  (let [log-file (io/file "./public/log" (str udid ".log"))]
    (with-open [writer (io/writer log-file :append true)]
      (.write writer (pr-str data)))))

(defn init []
  (ensure-logs-dir)
  #_(tel/add-handler!
     :dispatch-udid-file
     (fn
       ([signal]
        (let [udid (get-in signal [:ctx :udid])]
          (write-to-file udid (select-keys signal [:level :msg_ :data :ctx :inst])))
        #_([] (println "handler shutdown"))))))

(comment
  (init)
  :rcf)
