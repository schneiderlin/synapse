(ns com.zihao.replicant-component.ui.table)

(def refresh-button-classes ["btn" "btn-sm"])
(def new-button-classes ["btn" "btn-sm" "btn-primary" "ml-2"])

(defn table-ui
  "Modern table component with daisyUI styling.
   - :on-refresh: A handler for the refresh button, e.g., [[:data/query query]].
   - :on-new: An optional handler for the 'New' button. If provided, the button is displayed.
   - :theads: A list of table header names, e.g., [\"ID\", \"Username\", \"Balance\"].
   - :rows: A list of data for the table rows.
   - :row->tr: A map where keys are header names and values are functions to render the cell content.
   - :page: The current page number.
   - :total-pages: The total number of pages available.
   - :go-page: A function that takes a page number and returns a handler for pagination.
   - :multi-selection?: A boolean to enable/disable row selection checkboxes."
  [{:keys [on-refresh on-new
           theads
           rows row->tr row->id
           page go-page set-size
           multi-selection?
           select-all?
           table-id
           size]
    :or {on-refresh []
         multi-selection? false
         select-all? false
         page 1
         size 20
         rows []
         row->tr identity
         go-page (fn [p]
                   [[:debug/print "go-page" p]])
         set-size (fn [s]
                    [[:debug/print "set-size" s]])}
    :as _state}]
  [:div
   [:div {:class ["flex" "mb-4"]}
    [:button {:class refresh-button-classes
              :on {:click on-refresh}}
     "刷新"]
    (when on-new
      [:button {:class new-button-classes
                :on {:click on-new}}
       "新增"])]

   [:div {:class ["overflow-x-auto" "max-h-screen" "overflow-y-auto"]}
    [:table {:class ["table" "table-zebra" "w-full"]}
     [:thead {:class ["sticky" "top-0" "z-10" "bg-base-100"]}
      [:tr
       (when multi-selection?
         [:th [:label (if select-all?
                        [:input {:type "checkbox" :class ["checkbox"]
                                 :on {:change
                                      [[:table/toggle-all {:table-id table-id
                                                           :rows rows
                                                           :row->id row->id
                                                           :event :event/event
                                                           :selected? :event/target.checked}]]}}]
                        "")]])
       (for [thead theads]
         [:th thead])]]

     [:tbody #_{:class ["max-h-screen" "overflow-y-auto"]}
      (for [row rows]
        [:tr {:class ["hover"]}
         (when multi-selection?
           [:td
            [:label
             [:input {:type "checkbox" :class ["checkbox"]
                      :checked (:selected? row)
                      :on {:change
                           [[:table/select-row {:row row
                                                :table-id table-id
                                                :row->id row->id
                                                :event :event/event
                                                :selected? :event/target.checked}]]}}]]])
         (for [thead theads]
           [:td ((row->tr thead) row)])])]]]

   ;; Pagination
   [:div {:class ["join" "mt-4" "flex" "justify-center"]}
    [:button {:class ["join-item" "btn"]
              :disabled (= page 1)
              :on {:click (go-page (dec page))}}
     "«"]
    [:button {:class ["join-item" "btn" "btn-active"]} (str "Page " page)]
    [:button {:class ["join-item" "btn"]
              :on {:click (go-page (inc page))}}
     "»"]
    [:div {:class ["join-item" "flex" "items-center" "gap-2" "px-3"]}
     [:span {:class ["text-sm"]} "跳转到"]
     [:input {:type "number"
              :class ["input" "input-sm" "w-16"]
              :placeholder (str page)
              :min "1"
              :on {:blur (go-page :event/target.int-value)}}]]
    [:div {:class ["join-item" "flex" "items-center" "gap-2" "px-3"]}
     [:span {:class ["text-sm"]} "每页"]
     [:select {:class ["select" "select-sm" "w-20"]
               :on {:change (set-size :event/target.int-value)}}
      [:option {:value 10} "10"]
      [:option {:value 20} "20"]
      [:option {:value 50} "50"]
      [:option {:value 100} "100"]
      [:option {:value 200} "200"]
      [:option {:value 500} "500"]]
     [:span {:class ["text-sm"]} "条"]]]])
