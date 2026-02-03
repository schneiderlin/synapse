(ns com.zihao.language-learn.lingq.render
  (:require
   [com.zihao.replicant-main.interface :as rm]
   [clojure.string :as str]
   [com.zihao.language-learn.lingq.common :refer [prefix]]
   [com.zihao.language-learn.lingq.article :refer [article-ui]]
   [com.zihao.language-learn.lingq.word-rating :refer [word-rating-ui]]))

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
  (let [{:keys [selected-word tokens preview-word preview-translation]} (prefix state)
        word-rating-query {:query/kind :query/get-word-rating}
        word->rating (rm/get-result state word-rating-query)
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
     (when preview-word
       [:div {:class ["flex" "flex-col" "space-y-4" "w-full" "max-w-md"]}
        [:div {:class ["border" "border-gray-300" "rounded-lg" "p-4"]}
         [:h3 {:class ["text-lg" "font-semibold" "mb-4"]} "Word Preview"]
         [:div {:class ["space-y-2"]}
          [:div
           [:span {:class ["font-medium"]} "原文: "]
           [:span preview-word]]
          [:div
           [:span {:class ["font-medium"]} "译文: "]
           (println "preview-translation" preview-translation)
           [:span (if (and preview-translation (sequential? preview-translation))
                    (clojure.string/join ", " preview-translation)
                    "Loading...")]]
          [:button {:class ["mt-4" "px-4" "py-2" "bg-blue-600" "text-white" "rounded-lg" "hover:bg-blue-700" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500"]
                    :on {:click [[:lingq/add-preview-word-to-database]]}}
           "添加到数据库"]]]])
     (when (and selected-word (not preview-word))
       (word-rating-ui {:word (str/lower-case selected-word)
                        :current-rating (get word->rating (str/lower-case selected-word))}))]))
