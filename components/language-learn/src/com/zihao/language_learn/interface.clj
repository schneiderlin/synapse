(ns com.zihao.language-learn.interface
  (:require
   [com.zihao.language-learn.fsrs.actions :as fsrs-actions]
   [com.zihao.language-learn.fsrs.api :as fsrs-api]
   [com.zihao.language-learn.lingq.api :as lingq-api]
   [com.zihao.language-learn.lingq.actions :as lingq-actions]))

(defn execute-action
  "Execute action for language-learn component (fsrs + lingq)"
  [system event action args]
  (or (fsrs-actions/execute-action system event action args)
      (lingq-actions/execute-action system event action args)))

(defn query-handler
  "Query handler for language-learn component (fsrs + lingq)"
  [system query]
  (or (fsrs-api/query-handler system query)
      (lingq-api/query-handler system query)))

(defn command-handler
  "Command handler for language-learn component (fsrs + lingq)"
  [system command]
  (or (fsrs-api/command-handler system command)
      (lingq-api/command-handler system command)))
