(ns com.zihao.language-learn.fsrs.actions
  (:require
   [com.zihao.language-learn.fsrs.common :refer [prefix]]))

(defn load-due-cards []
  [[:data/query {:query/kind :query/due-cards}
    {:on-success (fn [cards]
                   [[:store/assoc-in [prefix :due-cards] cards]
                    [:store/assoc-in [prefix :current-card-index] 0]
                    [:store/assoc-in [prefix :show-back] false]
                    [:store/assoc-in [prefix :loading?] false]])}]
   [:store/assoc-in [prefix :loading?] true]])

(comment
  (load-due-cards)
  :rcf)

(defn repeat-card [store {:keys [id rating]}]
  [[:data/command 
    {:command/kind :command/repeat-card
     :command/data {:id id :rating rating}}
    {:on-success (fn [_result] 
                   (let [current-index (get-in @store [prefix :current-card-index])
                         due-cards (get-in @store [prefix :due-cards])
                         next-index (inc current-index)] 
                     (if (< next-index (count due-cards))
                       ;; Move to next card
                       [[:store/assoc-in [prefix :current-card-index] next-index]
                        [:store/assoc-in [prefix :show-back] false]]
                       ;; All cards reviewed, reload
                       [[:store/assoc-in [prefix :due-cards] []]
                        [:store/assoc-in [prefix :current-card-index] 0]
                        [:store/assoc-in [prefix :show-back] false]
                        [:data/query {:query/kind :query/due-cards}
                         {:on-success (fn [cards]
                                        [[:store/assoc-in [prefix :due-cards] cards]
                                         [:store/assoc-in [prefix :loading?] false]])}]])))}]])

(defn show-answer []
  [[:store/assoc-in [prefix :show-back] true]])

(defn execute-action [{:keys [store] :as _system} _e action args] 
  (case action
    :card/load-due-cards (load-due-cards)
    :card/repeat-card (repeat-card store args)
    :flashcard/show-answer (show-answer)
    nil))
