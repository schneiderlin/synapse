(ns com.zihao.replicant-component.component.server-select-filter
  (:require
   [clojure.string :as str]
   [com.zihao.replicant-main.replicant.query :as query]))

(def ^:private search-timers (atom {}))

(defn- throttle-search 
  "Throttle search action to avoid too many server requests"
  [filter-key delay-ms dispatch-fn]
  (when-let [timer (get @search-timers filter-key)]
    #?(:cljs (js/clearTimeout timer)))
  
  (let [timer #?(:cljs (js/setTimeout 
                        (fn []
                           (swap! search-timers dissoc filter-key)
                           (dispatch-fn))
                        delay-ms)
                 :clj (future 
                        (Thread/sleep delay-ms)
                        (swap! search-timers dissoc filter-key)
                        (dispatch-fn)))]
    (swap! search-timers assoc filter-key timer)))

(defn execute-action
  "Handle server-filter actions"
  [store _event action args]
  (case action
    :server-filter-search
    (let [{:keys [filter-key search-term query throttle-ms]} args
          throttle-delay (or throttle-ms 500)]
      (println "search-term" search-term)
      (when-not (empty? search-term)
        ;; The throttle function will handle the delay and execute the action
        (throttle-search filter-key throttle-delay
                         (fn [] 
                           (let [execute-action-f (:dispatch @store)]
                             (execute-action-f store nil [[:data/query query]]))))))
    nil))

(defn server-select-filter
  "Server-side searchable select component with throttled search
   
   state: application state
   prefix: state path prefix for storing filter data
   query: query map to trigger for server search
   & options:
   :label - display label
   :placeholder - placeholder text
   :throttle-ms - throttle delay in ms (default 500)
   :local-options - static options to show initially"
  [state prefix query & {:keys [label placeholder throttle-ms local-options
                                search-option->label search-option->value
                                on-select]
                         :or {label "选择选项"
                              placeholder "搜索选项..."
                              search-option->label identity
                              search-option->value identity
                              throttle-ms 500
                              local-options []}}]
  (let [search-term (get-in state [prefix :search] "")
        selected-value (get-in state [prefix :value])
        is-open (get-in state [prefix :dropdown-open] false)

        ;; Get server results from query
        server-options (or (query/get-result state query) [])

        ;; Combine local and server options
        all-options (concat local-options server-options)

        ;; Filter options based on search term
        filtered-options (if (empty? search-term)
                           all-options
                           (filter #(str/includes?
                                     (str/lower-case (search-option->label %))
                                     (str/lower-case search-term))
                                   all-options))

        selected-label (when selected-value
                         (search-option->label (first (filter #(= (search-option->value %) selected-value) all-options))))
        display-value (or selected-label search-term "")]

    [:div.form-control
     [:label.label
      [:span.label-text label]]
     [:div.relative
      [:input {:type "text"
               :placeholder placeholder
               :value display-value
               :class ["input" "input-bordered" "w-full" "pr-10"]
               :on {:input [[:store/assoc-in [prefix :search] :event/target.value]
                            [:store/assoc-in [prefix :value] nil]
                            [:store/assoc-in [prefix :dropdown-open] true]
                            [:server-filter-search {:filter-key prefix
                                                    :search-term :event/target.value
                                                    :query query
                                                    :throttle-ms throttle-ms}]]
                     :focus [[:store/assoc-in [prefix :dropdown-open] true]]
                      }}]
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
                    :class ["relative" "py-2" "pl-3" "pr-9" "cursor-pointer" "hover:bg-gray-100"
                            (when (= selected-value (search-option->value option)) " bg-blue-50")]
                    :on {:click (let [base [[:store/assoc-in [prefix :value] (search-option->value option)]
                                                     [:store/assoc-in [prefix :search] ""]
                                                     [:store/assoc-in [prefix :dropdown-open] false]]]
                                       (if on-select
                                         (concat base on-select)
                                         base))}}
              [:span {:class ["block" "truncate" (when (= selected-value (search-option->value option)) " font-medium")]}
               (search-option->label option)]]))])]]))

