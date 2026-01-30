(ns com.zihao.llm-eval.interface
  (:require [com.zihao.llm-eval.db :as db]))

(def get-evaluation-by-id db/get-evaluation-by-id)
(def get-evaluations db/get-evaluations)
(def get-evaluation-stats db/get-evaluation-stats)
(def get-score-distributions db/get-score-distributions)
(def get-model-names db/get-model-names)
(def get-criteria-names db/get-criteria-names)
(def create-evaluation! db/create-evaluation!)
(def create-score! db/create-score!)
