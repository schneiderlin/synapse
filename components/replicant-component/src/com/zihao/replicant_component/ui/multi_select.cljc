(ns com.zihao.replicant-component.ui.multi-select
  (:require
   [clojure.string :as str]))

(defn execute-action [{:keys [store] :as system} event action args]
  (case action
    :multi-select/toggle-selection (let [[selections-path option-value] args]
                                     [[:store/update-in selections-path
                                       (fn [selections]
                                         (if (contains? selections option-value)
                                           (disj selections option-value)
                                           (conj (or selections #{}) option-value)))]])
    nil))

(defn multi-select [state option-specs selections-path]
  (let [selections (get-in state selections-path #{})]
    [:div {:class ["w-full"]}
     ;; Options list
     (when (seq option-specs)
       [:div {:class ["max-h-60" "overflow-y-auto" "border" "border-gray-300" "rounded-md" "bg-white" "shadow-lg"]}
        (for [option option-specs]
          (let [is-selected? (contains? selections (:value option))]
            [:div {:key (:value option)
                   :class ["px-3" "py-2" "hover:bg-gray-100" "transition-colors"]}
             [:label {:class ["flex" "items-center" "cursor-pointer"]}
              [:input {:type "checkbox"
                       :checked is-selected?
                       :class ["mr-3" "h-4" "w-4" "text-blue-600" "focus:ring-blue-500" "border-gray-300" "rounded"]
                       :on {:change [[:multi-select/toggle-selection selections-path (:value option)]]}}]
              [:span {:class ["text-gray-700"]} (:label option)]]]))])]))
