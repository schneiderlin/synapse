(ns com.zihao.llm-eval.interface)

;; Frontend interface
(def render-dashboard com.zihao.llm-eval.dashboard/dashboard-view)
(def render-dataset com.zihao.llm-eval.dataset/dataset-page)
(def render-detail com.zihao.llm-eval.detail/evaluation-detail)
(def execute-actions com.zihao.llm-eval.actions/execute-action)

;; Backend interface
(def command-handler com.zihao.llm-eval.api/command-handler)
(def query-handler com.zihao.llm-eval.api/query-handler)
