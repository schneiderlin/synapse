(ns com.zihao.language-learn.lingq.actions
  (:require
   [com.zihao.language-learn.lingq.common :refer [prefix]]))

(defn click-unknown-word [_store {:keys [word]}]
  [[:data/command {:command/kind :command/add-new-word
                   :command/data {:word word}}]])

(defn clean-text [_store]
  [[:store/assoc-in [prefix :tokens] []]
   [:store/assoc-in [prefix :selected-word] nil]])

(defn enter-article [_store {:keys [article]}]
  [[:data/query {:query/kind :query/tokenize-text
                 :query/data {:language "id"
                              :text article}}
    {:on-success [[:data/query {:query/kind :query/get-word-rating}
                   {:on-success [[:store/assoc-in [prefix :word->rating] :query/result]
                                 [:store/assoc-in [prefix :tokens] :query/result]]}]]}]])

(defn execute-action [{:keys [store]} _event action args]
  (case action
    :lingq/click-unknown-word (click-unknown-word store args)
    :lingq/clean-text (clean-text store)
    :lingq/enter-article (enter-article store args)
    nil))
