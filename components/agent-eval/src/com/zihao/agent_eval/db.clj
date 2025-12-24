(ns com.zihao.agent-eval.db
  (:require [next.jdbc :as jdbc]
            [honey.sql :as sql]))

(def db-spec
  "SQLite database specification"
  {:dbtype "sqlite"
   :dbname "databases/agent-eval.db"})

(defonce ^:private datasource
  (delay (jdbc/get-datasource db-spec)))

(defn get-datasource []
  @datasource)

(comment
  (jdbc/execute! (get-datasource) ["SELECT 1"])
  (jdbc/execute-one! (get-datasource) ["SELECT name FROM sqlite_master WHERE type='table';"])
  :rcf)

(defn insert-log!
  "Insert a log entry into the database"
  [{:keys [input output session_id extra timestamp]}]
  (let [sql-map {:insert-into :llm_log
                 :values [{:input input
                           :output output
                           :session_id session_id
                           :extra extra
                           :timestamp timestamp}]}
        sql (sql/format sql-map)]
    (jdbc/execute! (get-datasource) sql)))

(comment
  (insert-log! {:input "test" :output "test" :session_id "test" :extra "test" :timestamp 1719168000}) 
  :rcf)

(defn get-all-logs
  "Get all logs, optionally limited"
  ([] (get-all-logs nil))
  ([limit]
   (let [sql-map (cond-> {:select :*
                          :from [:llm_log]
                          :order-by [[:timestamp :desc]]}
                   limit (assoc :limit limit))
         sql (sql/format sql-map)]
     (jdbc/execute! (get-datasource) sql))))

(comment
  (get-all-logs)
  :rcf)

(defn get-log-by-id
  "Get a single log by id"
  [id]
  (let [sql-map {:select :*
                 :from [:llm_log]
                 :where [:= :id id]}
        sql (sql/format sql-map)]
     (first (jdbc/execute! (get-datasource) sql))))

(comment
  (get-log-by-id 1)
  :rcf)

(defn get-logs-by-session
  "Get logs by session_id"
  [session-id]
  (let [sql-map {:select :*
                 :from [:llm_log]
                 :where [:= :session_id session-id]
                 :order-by [[:timestamp :desc]]}
        sql (sql/format sql-map)]
    (jdbc/execute! (get-datasource) sql)))

(comment
  (get-logs-by-session "test")
  :rcf)

