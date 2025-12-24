(ns com.dx.replicant-component.component.server-filter
  (:require
   [clojure.string :as str]))

(defn select-filter
  "Searchable select component with combined input/dropdown"
  [state prefix options & {:keys [label placeholder
                                  on-search]
                           :or {label "选择选项"
                                placeholder "搜索选项..."
                                on-search [[:store/assoc-in [prefix :search] :event/target.value]
                                           [:store/assoc-in [prefix :value] nil]
                                           [:store/assoc-in [prefix :dropdown-open] true]]}}]
  (let [search-term (get-in state [prefix :search] "")
        selected-value (get-in state [prefix :value])
        is-open (get-in state [prefix :dropdown-open] false)
        filtered-options (if (empty? search-term)
                           options
                           (filter #(str/includes?
                                     (str/lower-case (:label %))
                                     (str/lower-case search-term))
                                   options))
        selected-label (when selected-value
                         (:label (first (filter #(= (:value %) selected-value) options))))
        display-value (or selected-label search-term "")]
    [:div.form-control
     [:label.label
      [:span.label-text label]]
     [:div.relative
      [:input {:type "text"
               :placeholder placeholder
               :value display-value
               :class ["input" "input-bordered" "w-full" "pr-10"]
               :on {:input on-search
                    :focus [[:store/assoc-in [prefix :dropdown-open] true]]
                    :blur [[:store/assoc-in [prefix :dropdown-open] false]]}}]
      [:button {:type "button"
                :class ["absolute" "inset-y-0" "right-0" "flex" "items-center" "pr-3"]
                :on {:click [[:store/assoc-in [prefix :dropdown-open] (not is-open)]]}}
       [:svg.h-5.w-5.text-gray-400
        {:viewBox "0 0 20 20" :fill "currentColor"}
        [:path {:fill-rule "evenodd"
                :d "M5.293 7.293a1 1 0 011.414 0L10 10.586l3.293-3.293a1 1 0 111.414 1.414l-4 4a1 1 0 01-1.414 0l-4-4a1 1 0 010-1.414z"
                :clip-rule "evenodd"}]]]
      (when is-open
        [:div.absolute.z-10.mt-1.w-full.bg-white.shadow-lg.max-h-60.rounded-md.py-1.text-base.ring-1.ring-black.ring-opacity-5.overflow-auto
         (if (empty? filtered-options)
           [:div.relative.py-2.pl-3.pr-9.text-gray-500
            "没有找到匹配的选项"]
           (for [option filtered-options]
             [:div {:key (:value option)
                    :class ["relative""py-2""pl-3""pr-9""cursor-pointer""hover:bg-gray-100"
                            (when (= selected-value (:value option)) "bg-blue-50")]
                    :on {:click [[:store/assoc-in [prefix :value] (:value option)]
                                 [:store/assoc-in [prefix :search] ""]
                                 [:store/assoc-in [prefix :dropdown-open] false]]}}
              [:span {:class ["block" "truncate" (when (= selected-value (:value option)) " font-medium")]}
               (:label option)]]))])]]))