(ns com.zihao.llm-eval.api
  (:require [com.zihao.llm-eval.db :as db]))

(defn query-handler
  [cmd]
  (case (:command/kind cmd)
    :query/evaluations
    (let [data (:command/data cmd)
          filters (or (:filter data) {})
          page (or (:page data) 1)
          size (or (:size data) 50)
          sort-field (or (get-in data [:sort :field]) :timestamp)
          sort-order (or (get-in data [:sort :order]) :desc)]
      {:command/kind :data/query-result
       :query-id (:query-id cmd)
       :result (db/get-evaluations filters page size sort-field sort-order)})

    :query/evaluation-by-id
    (let [data (:command/data cmd)
          id (:id data)]
      {:command/kind :data/query-result
       :query-id (:query-id cmd)
       :result (db/get-evaluation-by-id id)})

    :query/evaluation-stats
    (let [data (:command/data cmd)
          _filters (:filters data {})]
      {:command/kind :data/query-result
       :query-id (:query-id cmd)
       :result (db/get-evaluation-stats)})

    :query/score-distributions
    (let [data (:command/data cmd)
          _filters (:filters data {})]
      {:command/kind :data/query-result
       :query-id (:query-id cmd)
       :result (db/get-score-distributions)})

    :query/model-names
    {:command/kind :data/query-result
     :query-id (:query-id cmd)
     :result (db/get-model-names)}

    :query/criteria-names
    {:command/kind :data/query-result
     :query-id (:query-id cmd)
     :result (db/get-criteria-names)}

    (do
      (println "Unknown query kind:" (:command/kind cmd))
      {:command/kind :data/query-error
       :query-id (:query-id cmd)
       :error (str "Unknown query kind: " (:command/kind cmd))})))

(defn command-handler
  [cmd]
  (case (:command/kind cmd)
    :command/create-evaluation
    (let [data (:command/data cmd)
          evaluation {:evaluation/id (:id data)
                      :evaluation/input (:input data)
                      :evaluation/output (:output data)
                      :evaluation/model-name (:model-name data)
                      :evaluation/timestamp (or (:timestamp data) (java.util.Date.))
                      :evaluation/prompt-metadata (:prompt-metadata data)
                      :evaluation/scores []}]
      (db/create-evaluation! evaluation)
      {:command/kind :data/command-result
       :command-id (:command-id cmd)
       :result {:success true :id (:id data)}})

    :command/create-score
    (let [data (:command/data cmd)
          score {:evaluation-score/id (:id data)
                 :evaluation-score/criterion-name (:criterion-name data)
                 :evaluation-score/score-value (:score-value data)
                 :evaluation-score/judge-type (:judge-type data)
                 :evaluation-score/feedback (:feedback data)
                 :evaluation-score/evaluation [:evaluation/id (:evaluation-id data)]}]
      (db/create-score! score)
      {:command/kind :data/command-result
       :command-id (:command-id cmd)
       :result {:success true :id (:id data)}})

    (do
      (println "Unknown command kind:" (:command/kind cmd))
      {:command/kind :data/command-error
       :command-id (:command-id cmd)
       :error (str "Unknown command kind: " (:command/kind cmd))})))
