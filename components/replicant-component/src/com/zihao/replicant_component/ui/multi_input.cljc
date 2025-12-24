(ns com.zihao.replicant-component.ui.multi-input
  (:require
   [clojure.string :as str]))

(defn paste-value [items-path value]
  (let [new-items (mapv str/trim (str/split value #"\n"))] 
    [[:store/update-in items-path (fn [items]
                                    (if (nil? items)
                                      (into #{} new-items)
                                      (into items new-items)))]]))

(defn execute-action [store event action args]
  (case action
    :multi-input/paste-value (let [[items-path value] args]
                               (paste-value items-path value))
    nil))

(defn multi-input [state items-path]
  (let [items (get-in state items-path #{})]
    [:div {:class ["w-full"]}
     ;; Input field
     [:input {:type "text"
              :class ["w-full" "px-3" "py-2" "border" "border-gray-300" "rounded-md" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500" "focus:border-transparent"]
              :on {:keypress [[:key/press
                               "Enter"
                               [[:store/assoc-in items-path (conj items :event/target.value)]
                                [:event/clear-input]]]]
                   :paste [[:multi-input/paste-value items-path :event/clipboard-data]]}}]

     ;; Items list
     (when (seq items)
       [:div {:class ["mt-3" "space-y-2"]}
        (for [item items]
          [:div {:class ["group" "flex" "items-center" "justify-between" "px-3" "py-2" "bg-gray-100" "rounded-md" "hover:bg-gray-200" "transition-colors"]}
           [:span {:class ["text-gray-700"]} item]
           [:button {:class ["opacity-0" "group-hover:opacity-100" "ml-2" "px-2" "py-1" "text-red-500" "hover:text-red-700" "hover:bg-red-100" "rounded" "transition-all" "duration-200"]
                     :on {:click [[:store/assoc-in items-path (disj items item)]]}}
            "x"]])])]))
