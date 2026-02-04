(ns com.zihao.language-learn.lingq.actions
  (:require
   [com.zihao.replicant-main.interface :refer [defaction]]
   [com.zihao.language-learn.lingq.common :refer [prefix]]))

(defn click-unknown-word [_store {:keys [word]}]
  (println "click-unknown-word" word)
  [[:store/assoc-in [prefix :preview-word] word]
   [:store/assoc-in [prefix :preview-translation] nil]
   [:data/query {:query/kind :query/get-word-translation
                 :query/data {:word word}}
    {:on-success [[:store/assoc-in [prefix :preview-translation] :query/result]]}]])

(defn add-preview-word-to-database [store]
  (let [word (get-in @store [prefix :preview-word])]
    [[:data/command {:command/kind :command/add-new-word
                     :command/data {:word word}}
      {:on-success [[:store/assoc-in [prefix :preview-word] nil]
                    [:store/assoc-in [prefix :preview-translation] nil]
                    [:data/query {:query/kind :query/get-word-rating}]]}]]))

(defn clean-text [_store]
  [[:store/assoc-in [prefix :tokens] []]
   [:store/assoc-in [prefix :selected-word] nil]
   [:store/assoc-in [prefix :input-text] nil]])

(defn enter-article [store]
  (let [article (get-in @store [prefix :input-text])]
    [[:data/query {:query/kind :query/tokenize-text
                   :query/data {:language "id"
                                :text article}}
      {:on-success [[:debug/print "tokenized text" :query/result]
                    [:store/assoc-in [prefix :tokens] :query/result]
                    [:data/query {:query/kind :query/get-word-rating}
                     {:on-success [[:debug/print "word rating" :query/result]
                                   [:store/assoc-in [prefix :word->rating] :query/result]]}]]}]]))

(defaction set-tokens
  [_store {:keys [tokens]}]
  [[:store/assoc-in [prefix :tokens] tokens]])

(defaction set-preview [_store {:keys [word translation]}] 
  [[:store/assoc-in [prefix :preview-word] word]
   [:store/assoc-in [prefix :preview-translation] translation]])

(defaction select-word [_store {:keys [word]}] 
  [[:store/assoc-in [prefix :selected-word] word]])

(defn execute-action [{:keys [store]} _event action args]
  (case action
    :lingq/click-unknown-word (click-unknown-word store (first args))
    :lingq/clean-text (clean-text store)
    :lingq/enter-article (enter-article store)
    :lingq/add-preview-word-to-database (add-preview-word-to-database store)
    :lingq/set-tokens (set-tokens store (first args))
    :lingq/set-preview (set-preview store (first args))
    :lingq/select-word (select-word store (first args))
    nil))
