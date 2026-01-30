(ns com.zihao.llm-eval.dashboard
  (:require [com.zihao.llm-eval.stats :as stats]))

(defn dashboard-view [state]
  (let [eval-stats (get-in state [:llm-eval :stats])
        score-distributions (get-in state [:llm-eval :score-distributions])]
    [:div {:class ["container" "mx-auto" "p-6"]}
     [:h1 {:class ["text-3xl" "font-bold" "mb-6"]} "LLM Evaluation Dashboard"]

     [:div {:class ["grid" "grid-cols-1" "md:grid-cols-4" "gap-4" "mb-8"]}
      [:div {:class ["stat" "bg-base-100" "shadow-sm" "rounded-lg"]}
       [:div {:class ["stat-title"]} "Total Evaluations"]
       [:div {:class ["stat-value" "text-2xl"]} (:total-evaluations eval-stats 0)]
       [:div {:class ["stat-desc"]} "All time"]]

      [:div {:class ["stat" "bg-base-100" "shadow-sm" "rounded-lg"]}
       [:div {:class ["stat-title"]} "Models"]
       [:div {:class ["stat-value" "text-2xl"]} (count (:model-counts eval-stats {}))]
       [:div {:class ["stat-desc"]} "Unique models"]]

      [:div {:class ["stat" "bg-base-100" "shadow-sm" "rounded-lg"]}
       [:div {:class ["stat-title"]} "Criteria"]
       [:div {:class ["stat-value" "text-2xl"]} (count (:avg-scores eval-stats {}))]
       [:div {:class ["stat-desc"]} "Evaluation criteria"]]

      [:div {:class ["stat" "bg-base-100" "shadow-sm" "rounded-lg"]}
       [:div {:class ["stat-title"]} "Avg Score"]
       [:div {:class ["stat-value" "text-2xl"]}
        (if-let [scores (:avg-scores eval-stats)]
          (let [avg (if (seq scores)
                      (/ (reduce + (vals scores)) (count scores))
                      0)]
            (str (Math/round (* 100 avg)) "/100"))
          "N/A")]
       [:div {:class ["stat-desc"]} "Overall average"]]]

     [:div {:class ["grid" "grid-cols-1" "lg:grid-cols-2" "gap-6" "mb-8"]}
      [:div {:class ["card" "bg-base-100" "shadow-sm"]}
       [:div {:class ["card-header"]}
        [:h2 {:class ["card-title"]} "Model Distribution"]]
       [:div {:class ["card-body"]}
        (if-let [model-counts (:model-counts eval-stats)]
          (stats/model-count-bar-chart model-counts)
          [:p {:class ["text-gray-500"]} "No data available"])]]

      [:div {:class ["card" "bg-base-100" "shadow-sm"]}
       [:div {:class ["card-header"]}
        [:h2 {:class ["card-title"]} "Average Scores by Criterion"]]
       [:div {:class ["card-body"]}
        (if-let [avg-scores (:avg-scores eval-stats)]
          [:div {:class ["space-y-3"]}
           (for [[criterion avg] (sort-by val > avg-scores)]
             [:div {:key criterion
                    :class ["flex" "items-center" "gap-3"]}
              [:span {:class ["w-32" "text-sm"]} criterion]
              [:div {:class ["flex-1" "bg-gray-200" "rounded-full" "h-6"]}
               [:div {:class ["bg-green-500" "h-6" "rounded-full" "flex" "items-center" "px-2" "text-xs" "text-white"]}
                {:style {:width (str (* 100 avg) "%")}}
                (str (Math/round (* 100 avg)))]]])]

          [:p {:class ["text-gray-500"]} "No data available"])]]]

     [:div {:class ["card" "bg-base-100" "shadow-sm"]}
      [:div {:class ["card-header"]}
       [:h2 {:class ["card-title"]} "Score Distributions"]]
      [:div {:class ["card-body"]}
       (if (seq score-distributions)
         [:div {:class ["space-y-4"]}
          (for [{:keys [criterion-name judge-type distribution total avg]} score-distributions]
            [:div {:key (str criterion-name "-" judge-type)
                   :class ["border" "rounded-lg" "p-4"]}
             [:div {:class ["flex" "items-center" "justify-between" "mb-2"]}
              [:div
               [:h3 {:class ["font-medium"]} criterion-name]
               [:p {:class ["text-sm" "text-gray-600"]}
                (str "Judge: " judge-type " | Total: " total " | Avg: " avg "/100")]]]

             (stats/score-distribution-bar {:distribution distribution :total total})])]

         [:p {:class ["text-gray-500"]} "No score distribution data available"])]]]))
