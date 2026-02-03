(ns com.zihao.llm-eval.detail
  (:require [com.zihao.llm-eval.stats :as stats]))

(defn evaluation-detail [state]
  (let [_evaluation-id (get-in state [:location :params :id])
        evaluation (get-in state [:llm-eval :evaluation-detail])]
    [:div {:class ["container" "mx-auto" "p-6"]}
     [:div {:class ["flex" "items-center" "gap-4" "mb-6"]}
      [:button {:class ["btn" "btn-ghost"]
                :on {:click [[:router/navigate [:llm-eval/dataset]]]}}
       "← Back"]
      [:h1 {:class ["text-3xl" "font-bold"]} "Evaluation Detail"]]

     (if evaluation
       [:div {:class ["space-y-6"]}
        [:div {:class ["card" "bg-base-100" "shadow-sm"]}
         [:div {:class ["card-header"]}
          [:h2 {:class ["card-title"]} "Evaluation ID: " (:evaluation/id evaluation)]]
         [:div {:class ["card-body"]}
          [:div {:class ["grid" "grid-cols-1" "md:grid-cols-2" "gap-4"]}
           [:div
            [:label {:class ["label"]}
             [:span {:class ["label-text" "font-medium"]} "Model"]]
            [:p {:class ["text-lg" "font-bold"]}
             (:evaluation/model-name evaluation)]]
           [:div
            [:label {:class ["label"]}
             [:span {:class ["label-text" "font-medium"]} "Timestamp"]]
            [:p {:class ["text-lg"]}
             (stats/format-date (:evaluation/timestamp evaluation))]]]]]

        [:div {:class ["card" "bg-base-100" "shadow-sm"]}
         [:div {:class ["card-header"]}
          [:h2 {:class ["card-title"]} "Input"]]
         [:div {:class ["card-body"]}
          [:pre {:class ["bg-gray-100" "p-4" "rounded-lg" "overflow-x-auto" "text-sm" "font-mono"]}
           (:evaluation/input evaluation)]]]

        [:div {:class ["card" "bg-base-100" "shadow-sm"]}
         [:div {:class ["card-header"]}
          [:h2 {:class ["card-title"]} "Output"]]
         [:div {:class ["card-body"]}
          [:pre {:class ["bg-gray-100" "p-4" "rounded-lg" "overflow-x-auto" "text-sm" "font-mono"]}
           (:evaluation/output evaluation)]]]

        [:div {:class ["card" "bg-base-100" "shadow-sm"]}
         [:div {:class ["card-header"]}
          [:h2 {:class ["card-title"]} "Scores"]]
         [:div {:class ["card-body"]}
          (if (seq (:evaluation/scores evaluation))
            [:div {:class ["overflow-x-auto"]}
             [:table {:class ["table" "table-zebra"]}
              [:thead
               [:tr
                [:th "Criterion"]
                [:th "Score"]
                [:th "Judge Type"]
                [:th "Feedback"]]]

              [:tbody
               (for [score (:evaluation/scores evaluation)]
                 [:tr {:key (:evaluation-score/id score)}
                  [:td (:evaluation-score/criterion-name score)]
                  [:td
                   [:span {:class ["badge" (if (< (:evaluation-score/score-value score) 7)
                                             "badge-error"
                                             "badge-success")]}
                    (str (Math/round (* 100 (:evaluation-score/score-value score))) "/100")]]

                  [:td
                   [:span {:class ["badge" "badge-ghost"]}
                    (:evaluation-score/judge-type score)]]

                  [:td (:evaluation-score/feedback score)]])]]]

            [:p {:class ["text-gray-500"]} "No scores available"])]]

        [:div {:class ["card" "bg-base-100" "shadow-sm"]}
         [:div {:class ["card-header"]}
          [:h2 {:class ["card-title"]} "Prompt Metadata"]]
         [:div {:class ["card-body"]}
          [:pre {:class ["bg-gray-100" "p-4" "rounded-lg" "overflow-x-auto" "text-xs" "font-mono"]}
           (:evaluation/prompt-metadata evaluation)]]]]

       [:div {:class ["alert" "alert-warning"]}
        [:span "Evaluation not found"]])]))
