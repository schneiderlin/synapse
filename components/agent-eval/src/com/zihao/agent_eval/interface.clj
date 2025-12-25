(ns com.zihao.agent-eval.interface
  (:require [com.zihao.agent-eval.collector :as collector]))

(defn create-collector
  "Create a BAML Collector instance."
  ([system] (collector/create-collector system))
  ([system name] (collector/create-collector system name)))

(defn extract-log-data
  "Extract relevant data from Collector FunctionLog"
  [collector]
  (collector/extract-log-data collector))

(defn extract-http-request-response
  "Extract HTTP request and response from Collector"
  [collector]
  (collector/extract-http-request-response collector))

(defn save-collector-log!
  "Save collector data to database"
  [collector]
  (collector/save-collector-log! collector))
