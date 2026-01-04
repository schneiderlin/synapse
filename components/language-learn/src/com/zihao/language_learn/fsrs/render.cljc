(ns com.zihao.language-learn.fsrs.render
  (:require
   [com.zihao.language-learn.fsrs.common :refer [prefix]]))

(defn progress-bar [{:keys [total-cards current-card-index]}]
  [:div {:class ["mb-4"]}
   [:div {:class ["flex" "justify-between" "text-sm" "text-gray-600"]}
    [:span (str "Card " (inc current-card-index) " of " total-cards)]]
   [:div {:class ["w-full" "bg-gray-200" "rounded-full" "h-2"]}
    [:div {:class ["bg-blue-600" "h-2" "rounded-full"]
           :style {:width (str (* 100 (/ (inc current-card-index) total-cards)) "%")}}]]])

(defn card-front [card]
  (:card/front card))

(defn card-back [card]
  (:card/back card))

(defn card-ui [{:keys [card show-back]}]
  [:div {:class ["bg-white" "rounded-lg" "shadow-lg" "p-8" "min-h-64" "flex" "items-center" "justify-center"]}
   [:div {:class ["text-center" "w-full"]}

    ;; Card content (front or back)
    [:div {:class ["text-xl" "mb-6" "min-h-20" "flex" "items-center" "justify-center"]}
     (if show-back
       (card-back card)
       (card-front card))]

    ;; Show answer button or rating buttons
    (if show-back
      ;; Rating buttons
      [:div {:class ["space-y-3"]}
       [:p {:class ["text-sm" "text-gray-600" "mb-4"]} "How well did you know this?"]
       [:div {:class ["grid" "grid-cols-2" "gap-3"]}
        [:button {:class ["btn" "btn-error" "btn-sm"]
                  :on {:click [[:card/repeat-card {:id (:db/id card) :rating :again}]]}}
         "Again (1)"]
        [:button {:class ["btn" "btn-warning" "btn-sm"]
                  :on {:click [[:card/repeat-card {:id (:db/id card) :rating :hard}]]}}
         "Hard (2)"]
        [:button {:class ["btn" "btn-success" "btn-sm"]
                  :on {:click [[:card/repeat-card {:id (:db/id card) :rating :good}]]}}
         "Good (3)"]
        [:button {:class ["btn" "btn-info" "btn-sm"]
                  :on {:click [[:card/repeat-card {:id (:db/id card) :rating :easy}]]}}
         "Easy (4)"]]]
      ;; Show answer button
      [:button {:class ["btn" "btn-primary" "btn-lg"]
                :on {:click [[:flashcard/show-answer]]}}
       "Show Answer"])]])

(defn flashcard-component [state]
  (let [form-state (prefix state)
        {:keys [current-card-index due-cards]} form-state
        card (get due-cards current-card-index)
        total-cards (count due-cards)]
    (when (and card due-cards)
      [:div {:class [ "mx-auto" "p-6"]}
       ;; Progress indicator
       (progress-bar {:current-card-index current-card-index
                      :total-cards total-cards})

       ;; Card content
       (card-ui {:card card
                 :show-back (:show-back form-state)})])))

(defn no-cards-message []
  [:div {:class ["max-w-2xl" "mx-auto" "p-6" "text-center"]}
   [:div {:class ["bg-green-50" "rounded-lg" "p-8"]}
    [:div {:class ["text-6xl" "mb-4"]} "🎉"]
    [:h2 {:class ["text-2xl" "font-bold" "text-green-800" "mb-2"]} "All caught up!"]
    [:p {:class ["text-green-600"]} "No cards are due for review right now."]
    [:div {:class ["flex" "gap-3" "justify-center" "mt-4"]}
     [:button {:class ["btn" "btn-primary"]
               :on {:click [[:card/load-due-cards]]}}
      "Refresh"]
     [:button {:class ["btn" "btn-secondary"]
               :on {:click [[:card/import-edn-file]]}}
      "Import EDN File"]]]])

(defn import-status-message [form-state]
  (cond
    (:import-success? form-state)
    [:div {:class ["alert" "alert-success" "mb-4"]}
     [:span "Cards imported successfully!"]]
    
    (:import-error? form-state)
    [:div {:class ["alert" "alert-error" "mb-4"]}
     [:span "Error importing cards. Please check the console for details."]]
    
    :else nil))

(defn flashcard-page [state]
  (let [form-state (prefix state)
        {:keys [due-cards loading?]} form-state]
    [:div {:class ["container" "mx-auto" "p-4"]}
     ;; Import status message
     (import-status-message form-state)
     
     (cond
       loading?
       [:div {:class ["flex" "justify-center" "items-center" "h-64"]}
        [:div {:class ["loading" "loading-spinner" "loading-lg"]}]]
       
       (empty? due-cards)
       (no-cards-message)
       
       :else
       (flashcard-component state))]))
