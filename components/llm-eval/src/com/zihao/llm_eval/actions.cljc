(ns com.zihao.llm-eval.actions)

(defn fetch-stats []
  [[:data/query {:query/kind :query/evaluation-stats}
    {:on-success (fn [stats]
                   [[:store/assoc-in [:llm-eval :stats] stats]])}]
   [:store/assoc-in [:llm-eval :loading-stats?] true]])

(defn fetch-score-distributions []
  [[:data/query {:query/kind :query/score-distributions}
    {:on-success (fn [distributions]
                   [[:store/assoc-in [:llm-eval :score-distributions] distributions]])}]])

(defn navigate-to-detail [id]
  [[:data/query {:query/kind :query/evaluation-by-id
                 :query/data {:id id}}
    {:on-success (fn [result]
                   [[:store/assoc-in [:llm-eval :evaluation-detail] result]])}]
   [:router/navigate [:llm-eval/dataset-detail {:id id}]]])

(defn execute-action [{:keys [store] :as _system} _event action args]
  (case action
    :llm-eval/fetch-stats (fetch-stats)
    :llm-eval/fetch-score-distributions (fetch-score-distributions)
    :llm-eval/navigate-to-detail (navigate-to-detail args)
    nil))
