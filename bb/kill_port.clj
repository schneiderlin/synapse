(ns kill-port
  (:require [babashka.process :as p :refer [shell pipeline pb]]
            [clojure.string :as str]))

(defn get-pids
  "Get all PIDs using the specified port.
   Returns a set of unique PIDs (as strings)."
  [port]
  (let [res @(p/process ["cmd" "/c" (str "netstat -aon | findstr " port)]
                        {:out :string
                         :err :string})
        output (:out res)]
    (if (str/blank? output)
      #{}
      (->> (str/split-lines output)
           (map (fn [line]
                  (let [parts (str/split (str/trim line) #"\s+")
                        pid (last parts)]
                    (when (and pid (re-matches #"\d+" pid))
                      pid))))
           (remove nil?)
           set))))

(comment
  (get-pids 8080)
  :rcf)

(defn kill-port 
  "Kill all processes using the specified port."
  [port]
  (let [pids (get-pids port)]
    (if (empty? pids)
      (println (str "No processes found using port " port))
      (doseq [pid pids]
        (println (str "Killing process " pid " on port " port))
        @(p/process ["taskkill" "/F" "/PID" pid]
                    {:out :string
                     :err :string})))))

(comment
  ;; Extract PIDs from a port
  (get-pids 42830)
  ;; => #{"70188" "52836"}
  
  ;; Kill all processes on a port
  (kill-port 8080) 
  :rcf)


