(ns com.zihao.agent-tools.interface
  (:require
   [com.zihao.agent-tools.core :as core]))

(defn grep-tool-fn [args]
  (core/grep-tool-fn args))

(defn list-dir-tool-fn [args]
  (core/list-dir-tool-fn args))

(defn glob-tool-fn [args]
  (core/glob-tool-fn args))
