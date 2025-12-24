(ns com.zihao.language-learn.lingq.render
  (:require 
   [replicant.query :as query]
   [clojure.string :as str]
   [lingq.common :refer [prefix]]
   [lingq.article :refer [article-ui]] 
   [lingq.word-rating :refer [word-rating-ui]]))

(defn textarea-ui []
  [:div {:class ["flex" "flex-col" "space-y-4" "w-full" "max-w-2xl"]}
   [:h3 {:class ["text-xl" "font-semibold" "mb-2"]} "Input Article Text"]
   [:textarea {:class ["w-full" "h-96" "p-4" "border" "border-gray-300" "rounded-lg" "resize-none" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500"]
               :placeholder "Paste or type your article text here..."
               :on {:input [[:debug/print :event/target.value]
                            [:lingq/enter-article {:article :event/target.value}]]}}]
   [:div {:class ["flex" "space-x-4"]}
    [:button {:class ["px-4" "py-2" "bg-blue-600" "text-white" "rounded-lg" "hover:bg-blue-700" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500"]
              :on {:click [[:lingq/clean-text]]}}
     "Clear Text"]]])

(defn main [state] 
  (let [{:keys [selected-word tokens]} (prefix state)
        word-rating-query {:query/kind :query/get-word-rating}
        word->rating (query/get-result state word-rating-query)
        has-text? (seq tokens)]
    [:div {:class ["flex" "place-content-evenly"]} 
     (if has-text?
       [:div {:class ["flex" "flex-col" "space-y-4"]}
        [:div {:class ["flex" "justify-between" "items-center"]}
         [:h3 {:class ["text-lg" "font-semibold"]} "Article Text"]
         [:button {:class ["px-3" "py-1" "bg-red-600" "text-white" "text-sm" "rounded" "hover:bg-red-700" "focus:outline-none" "focus:ring-2" "focus:ring-red-500"]
                   :on {:click [[:lingq/clean-text]]}}
          "Clear Article"]]
        (article-ui {:tokens tokens :word->rating word->rating})]
       (textarea-ui))
     (when selected-word
       (word-rating-ui {:word (str/lower-case selected-word)
                        :current-rating (get word->rating (str/lower-case selected-word))}))]))
