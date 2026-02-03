(ns com.zihao.llm-eval.router)

(def routes
  [[:pages/llm-eval-dashboard [["dashboard"]]]
   [:pages/llm-eval-dataset [["dataset"]]]
   [:pages/llm-eval-detail [["dataset" :id]]]
   [:pages/llm-eval-workflow [["workflow"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/llm-eval-dashboard [[:llm-eval/fetch-stats]
                               [:llm-eval/fetch-score-distributions]]
    :pages/llm-eval-dataset nil
    :pages/llm-eval-detail nil
    :pages/llm-eval-workflow nil
    nil))
