(ns com.zihao.llm-eval.dataset
  (:require [com.zihao.replicant-component.component.table :refer [table-component]]
            [com.zihao.llm-eval.stats :as stats]))

(defn dataset-page [_state]
  [:div {:class ["container" "mx-auto" "p-6"]}
   [:div {:class ["flex" "items-center" "justify-between" "mb-6"]}
    [:h1 {:class ["text-3xl" "font-bold"]} "Dataset"]
    [:div {:class ["flex" "gap-2"]}
     [:button {:class ["btn" "btn-primary"]
               :on {:click [[:llm-eval/fresh-data]]}}
      "Refresh"]]]

   (table-component :query/evaluations
                    ["Model" "Timestamp" "Input" "Output" "Avg Score" "Actions"]
                    {"Model" (fn [row]
                               [:span {:class ["badge" "badge-secondary"]}
                                (:evaluation/model-name row)])
                     "Timestamp" (fn [row]
                                   [:span {:class ["text-sm"]}
                                    (stats/format-date (:evaluation/timestamp row))])
                     "Input" (fn [row]
                               [:span {:class ["text-sm" "font-mono"]}
                                (stats/truncate-text (:evaluation/input row) 80)])
                     "Output" (fn [row]
                                [:span {:class ["text-sm" "font-mono"]}
                                 (stats/truncate-text (:evaluation/output row) 80)])
                     "Avg Score" (fn [row]
                                   [:span {:class ["badge" "badge-primary"]}
                                    (let [avg (stats/calculate-avg-score (:evaluation/scores row))]
                                      (str (Math/round (* 100 avg)) "/100"))])
                     "Actions" (fn [row]
                                 [:div {:class ["flex" "gap-2"]}
                                  [:button {:class ["btn" "btn-xs" "btn-outline"]
                                            :on {:click [[:llm-eval/navigate-to-detail (:evaluation/id row)]]}}
                                   "View"]])}
                    :table-id :evaluations-table
                    :row->id :evaluation/id)])
