(ns com.zihao.replicant-component.component.table-filter
  (:require
   [clojure.walk :as walk]
   [com.zihao.replicant-component.ui.table :refer [table-ui]]
   [replicant.query :as query]))

(defn get-filter-operators
  "Get available filter operators based on field type"
  [field-type]
  (case field-type
    :string [{:value "is" :label "is"}
             {:value "is not" :label "is not"}
             {:value "contains" :label "contains"}
             {:value "does not contain" :label "does not contain"}
             {:value "starts with" :label "starts with"}
             {:value "ends with" :label "ends with"}]
    :number [{:value "=" :label "="}
             {:value "!=" :label "!="}
             {:value ">" :label ">"}
             {:value ">=" :label ">="}
             {:value "<" :label "<"}
             {:value "<=" :label "<="}]
    #_#_:date [{:value "is" :label "is"}
           {:value "is not" :label "is not"}
           {:value "before" :label "before"}
           {:value "after" :label "after"}]
    []))

(defn filter-config-modal
  [{:keys [field]}]
  (let [field-name (:label field)
        operators (get-filter-operators (:type field))
        selected-operator (-> operators first :value)]
    [:div {:class ["w-80" "bg-white" "border" "border-gray-200" "rounded-lg" "shadow-xl" "z-50" "p-4"]}
     ;; Operator selection
     [:div {:class ["mb-4"]}
      [:div {:class ["space-y-2"]}
       (for [op operators]
         [:label {:class ["flex" "items-center" "space-x-2" "cursor-pointer"]}
          [:input {:type "radio"
                   :name "operator"
                   :value (:value op)}]
          [:span {:class ["ml-2" "text-sm"]}
           (:label op)]])]]

     ;; Value input
     [:form {:on {:submit [[:event/prevent-default]
                           [:table-filter/add-filter {:field field
                                                      :operator selected-operator
                                                      :from-value :event/form-data}]]}}
      [:div {:class ["mb-4"]}
       [:input {:type "text"
                :name "value"
                :placeholder (str "Enter " field-name " value...")
                :class ["input" "input-bordered" "w-full" "border-purple-500" "focus:border-purple-500"]}]]

      ;; Action buttons
      [:div {:class ["flex" "justify-end" "space-x-2"]}
       [:button {:class ["btn" "btn-sm" "btn-ghost"]
                 :on {:click [[:table-filter/cancel-new]]}}
        "Cancel"]
       [:button {:type "submit"
                 :class ["btn" "btn-sm" "btn-primary"]}
        "Apply Filter"]]]]))

(defn filter-dropdown
  [{:keys [fields is-open? 
           open-field]}]
  [:div {:class ["relative" "flex" "items-start"]}
   ;; Left side - Dropdown trigger and menu
   [:div {:class ["relative"]}
    ;; Dropdown trigger button
    [:button {:class ["btn" "btn-sm" "btn-outline" "flex" "items-center" "gap-2"]
              :on {:click [[:table-filter/open-new]]}}
     "Add Filter"
     [:svg {:class ["w-4" "h-4"]
            :fill "none"
            :stroke "currentColor"
            :viewBox "0 0 24 24"}
      [:path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M12 4v16m8-8H4"}]]]

    ;; Dropdown menu
    (when is-open?
      [:div {:class ["absolute" "top-full" "left-0" "mt-1" "w-48" "bg-white" "border" "border-gray-200" "rounded-md" "shadow-lg" "z-50"]}
       [:div {:class ["py-1"]}
        (for [field fields]
          [:button {:class ["w-full" "text-left" "px-4" "py-2" "text-sm" "hover:bg-gray-100" "flex" "items-center" "justify-between"]
                    :on {:click [[:table-filter/select-field field]]}}
           [:span (:label field)]
           [:span {:class ["text-xs" "text-gray-500"]} (name (:type field))]])]])]

   ;; Right side - Filter configuration modal
   (when open-field
     [:div {:class ["ml-4"]}
      (filter-config-modal {:field open-field})])])
