(ns com.zihao.llm-eval.stats)

(defn format-date [date]
  (if (instance? java.util.Date date)
    (let [formatter (java.text.SimpleDateFormat. "yyyy-MM-dd HH:mm")]
      (.format formatter date))
    date))

(defn truncate-text [text max-length]
  (if (and text (> (count text) max-length))
    (str (subs text 0 max-length) "...")
    (or text "")))

(defn calculate-avg-score [scores]
  (if (seq scores)
    (let [total (reduce + 0 (map :evaluation-score/score-value scores))]
      (/ total (count scores)))
    0))

(defn bar-chart [{:keys [data max-width height class]}]
  (let [max-value (if (seq data)
                    (apply max (map :value data))
                    1)
        bar-height (or height 24)
        bar-class (or class "bg-blue-500")]
    [:div {:class ["flex" "items-center" "gap-4" "w-full"]}
     [:div {:class ["flex-1" "flex" "gap-2"]}
      (for [{:keys [label value]} data]
        [:div {:key label
               :class ["flex-1" "flex" "flex-col" "items-center" "gap-1"]}
         [:span {:class ["text-xs" "text-gray-600"]} label]
         [:div {:class ["w-full" "rounded" bar-class]
                :style {:height (str bar-height "px")
                        :width (str (min 100 (* 100 (/ value max-value))) "%")}}]])]
     [:div {:class ["text-sm" "font-medium"]} (str max-value)]]))

(defn stats-card [{:keys [title value subtitle icon color]}]
  [:div {:class ["card" "bg-base-100" "shadow-sm"]}
   [:div {:class ["card-body" "p-4"]}
    [:div {:class ["flex" "items-center" "justify-between"]}
     [:div
      [:h3 {:class ["text-sm" "font-medium" "text-gray-600"]} title]
      [:p {:class ["text-2xl" "font-bold"]} value]]
     [:div {:class ["text-4xl"]} icon]]
    (when subtitle
      [:p {:class ["text-xs" "text-gray-500"]} subtitle])]])

(defn model-count-bar-chart [model-counts]
  (let [max-count (if (seq model-counts)
                    (apply max (vals model-counts))
                    1)]
    [:div {:class ["space-y-3"]}
     (for [[model count] (sort-by val > model-counts)]
       [:div {:key model
              :class ["flex" "items-center" "gap-3"]}
        [:span {:class ["w-24" "text-sm" "truncate"]} model]
        [:div {:class ["flex-1" "bg-gray-200" "rounded-full" "h-6"]}
         [:div {:class ["bg-primary" "h-6" "rounded-full" "flex" "items-center" "px-2" "text-xs" "text-white"]}
          {:style {:width (str (min 100 (* 100 (/ count max-count))) "%")}}
          (str count)]]])]))

(defn score-distribution-bar [{:keys [distribution total]}]
  (let [sorted-distribution (sort-by key distribution)
        max-count (if (seq sorted-distribution)
                    (apply max (vals sorted-distribution))
                    1)]
    [:div {:class ["flex" "items-center" "gap-1" "h-8"]}
     (for [[score count] sorted-distribution]
       [:div {:key score
              :class ["flex-1" "h-full" "rounded-sm" "bg-blue-500"]}
        {:style {:width (str (* 100 (/ count total)) "%")}
         :title (str "Score " score ": " count)}])]))
