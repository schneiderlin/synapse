(ns com.dx.agent-tools.interface
  (:require
   [com.dx.agent-tools.core :as core]))


(defn grep-tool-fn [{:keys [pattern path type]
                     :as args}]
  (core/grep-tool-fn args))

(defn list-dir-tool-fn [{:keys [target_directory]
                         :as args}]
  (core/list-dir-tool-fn args))

(defn glob-tool-fn [{:keys [glob_pattern target_directory]
                     :as args}]
  (core/glob-tool-fn args))