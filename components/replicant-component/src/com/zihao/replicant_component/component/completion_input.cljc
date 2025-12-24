(ns com.zihao.replicant-component.component.completion-input
  (:require
   [clojure.string :as str]))

(def options ["apple" "banana" "cherry" "date" "elderberry" "fig" "grape" "honeydew" "kiwi" "lemon" "lime" "mango" "melon" "nectarine" "orange" "pear" "pineapple" "plum" "pomegranate" "raspberry" "strawberry" "tangerine" "watermelon"])

(defn input-change [input-path options-path value]
  [#_[:store/assoc-in options-path 
    (if (empty? value)
      []
      (filter #(str/includes? (str/lower-case %) (str/lower-case value)) options))]
   [:data/send {:event-id :event/completion-input-change
                :event-data {:value value}}]
   [:store/assoc-in input-path value]])

(defn execute-action [store event action args]
  (case action
    :completion-input/input-change (let [[input-path options-path value] args]
                                     (input-change input-path options-path value))
    nil))

(defn completion-input [state options-path input-path]
  (let [options (get-in state options-path #{})
        input (get-in state input-path "")]
    [:div {:class ["w-full"]}
     [:input {:type "text"
              :class ["w-full" "px-3" "py-2" "border" "border-gray-300" "rounded-md" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500" "focus:border-transparent" "pr-10"]
              :value input
              :on {:input [[:completion-input/input-change input-path options-path :event/target.value]]}}]
     (when (seq options)
       [:div {:class ["mt-3" "space-y-2"]}
        (for [option options]
          [:div {:class ["group" "flex" "items-center" "justify-between" "px-3" "py-2" "bg-gray-100" "rounded-md" "hover:bg-gray-200" "transition-colors"]}
           [:span {:class ["text-gray-700"]} option]
           [:button {:class ["opacity-0" "group-hover:opacity-100" "ml-2" "px-2" "py-1" "text-red-500" "hover:text-red-700" "hover:bg-red-100" "rounded" "transition-all" "duration-200"]
                     :on {:click [[:completion-input/select-option input-path option]]}} "x"]])])]))
