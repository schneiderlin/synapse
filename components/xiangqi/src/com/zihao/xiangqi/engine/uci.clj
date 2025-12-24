(ns com.zihao.xiangqi.engine.uci
  (:require
   [missionary.core :as m])
  (:import [java.lang ProcessBuilder]
           [java.io InputStreamReader BufferedReader OutputStreamWriter BufferedWriter]))

(defn start-engine
  "Starts the UCI engine process."
  [engine-path]
  (let [pb (ProcessBuilder. [engine-path])
        process (.start pb)
        stdin (BufferedWriter. (OutputStreamWriter. (.getOutputStream process)))
        stdout (BufferedReader. (InputStreamReader. (.getInputStream process)))
        stderr (BufferedReader. (InputStreamReader. (.getErrorStream process)))]
    {:process process :stdin stdin :stdout stdout :stderr stderr}))

(comment
  (def engine
    (start-engine "/home/zihao/workspace/private/agent-from-scratch/data/pikafish/pikafish-avx2"))
  :rcf)

(defn send-command
  "Sends a command string to the engine's stdin."
  [{:keys [stdin]} command-string]
  (doto stdin
    (.write command-string)
    (.newLine)
    (.flush)))

(comment
  (send-command engine "uci")
  :rcf)

(defn stream-lines
  "Returns a Missionary flow that emits lines read from the engine's stdout.
   The flow terminates when the stream closes (readLine returns nil)."
  [{:keys [^BufferedReader stdout] :as _engine}]
  (m/ap
   (loop []
     (let [line (m/? (m/via m/blk (.readLine stdout)))]
       (if (nil? line)
         (do (println "Engine stdout stream closed.")
             (m/amb))
         (do
           (println "engine: " line)
           (m/amb line (recur))))))))

(defn engine->bestmove-flow [engine]
  (let [pattern #"^bestmove (\w+)"]
    (m/reductions (fn [acc line]
                    (if-let [move (re-find pattern line)]
                      (second move)
                      acc))
                  nil
                  (stream-lines engine))))

(defn stop-engine
  "Stops the engine process."
  [{:keys [process] :as engine}]
  (try
    (send-command engine "quit")
    ;; Optionally wait a moment or check if process exited
    (.destroy process)
    (catch Exception e
      (println "Error stopping engine:" (.getMessage e))
      ;; Force destroy if quit fails
      (.destroyForcibly process))))

(defn watch-bestmove [!bestmove engine]
  (let [main (m/reduce (fn [_ bestmove]
                         (reset! !bestmove bestmove))
                       nil
                       (engine->bestmove-flow engine))
        cancel (main println println)]
    cancel))

(comment
  ;; 公司
  (def engine
    (start-engine "/home/zihao/workspace/private/agent-from-scratch/data/pikafish/pikafish-avx2"))
  
  (def engine
    (start-engine "/home/linzihao/Desktop/workspace/private/agent-from-scratch/data/pikafish/pikafish-avx2")) 
  
  (def flow (engine->bestmove-flow engine))
  (send-command engine "isready")
  (send-command engine "position startpos")
  (send-command engine "position fen 9/9/3k5/9/9/9/4R4/3A5/8r/4K4 b - - 0 1")
  (send-command engine "d")
  (send-command engine "moves h2e2")
  (send-command engine "go")
  (send-command engine "stop")

  (def !bestmove (atom nil))
  (watch-bestmove !bestmove engine) 
  
  :rcf)
