(ns com.zihao.replicant-component.component.table
  (:require
   [clojure.walk :as walk]
   [com.zihao.replicant-component.ui.table :refer [table-ui]]
   [com.zihao.replicant-main.interface :as query]))

(defn- select-row [{:keys [row selected? table-id]}]
  (let [row (dissoc row :selected?)]
    [[:store/update-in [:tables table-id :selected] (fn [s]
                                                      (if selected?
                                                        (conj s row)
                                                        (disj s row)))]]))

(defn- clear-selected-ids [table-id]
  [[:store/assoc-in [:tables table-id :selected] #{}]])

(defn- toggle-all [{:keys [table-id rows selected?]}]
  (if selected?
    [[:store/update-in [:tables table-id :selected] concat rows]]
    (clear-selected-ids table-id)))

(defn execute-action [_store _event action args]
  (case action
    :table/clear-selected-ids (let [[table-id] args]
                                (clear-selected-ids table-id))
    :table/select-row (select-row args)
    :table/toggle-all (toggle-all args)
    nil))

(defn get-page [state table-id]
  (get-in state [:tables table-id :page] 1))

(defn get-size [state table-id]
  (get-in state [:tables table-id :size] 20))

(defn get-filter [state table-id]
  (get-in state [:tables table-id :filters] {}))

(defn get-rows [state table-id query-kind]
  (let [page (get-page state table-id)
        size (get-size state table-id)
        filter (get-filter state table-id)
        query {:query/kind query-kind
               :query/data {:page page :size size
                            :filter filter}}]
    (query/get-result state query)))

(defn get-selected-rows [state table-id]
  (get-in state [:tables table-id :selected] #{}))

(defn get-selected-ids [state table-id row->id]
  (into #{} (map
             row->id
             (get-selected-rows state table-id))))

(defn table-component
  "theads: list of table header name, e.g. [ID, 用户名, 余额]
    row->tr: a map, key is the same string as theads element, value is the correspond render function
    routes: global routes
    location: url location, use for pagination
    query-kind: query kind to query backend
    multi-selection?: whether the table supports multi-selection. recommand use with row->id
    row->id: a function to get the id of a row"
  [query-kind theads row->tr & {:keys [on-new
                                       row->id
                                       table-id
                                       multi-selection?
                                       select-all?]
                                :or {table-id query-kind
                                     row->id identity}}]
  (fn [state]
    (let [page (get-page state table-id)
          size (get-size state table-id)
          filter (get-filter state table-id)
          query {:query/kind query-kind
                 :query/data {:page page :size size
                              :filter filter}}
          rows (get-rows state table-id query-kind)
          go-page (fn [new-page]
                    [[:debug/print table-id new-page (str new-page)]
                     [:store/assoc-in [:tables table-id :page] new-page]])
          set-size (fn [new-size]
                     [#_[:debug/print table-id new-size (str new-size)]
                      [:store/assoc-in [:tables table-id :size] new-size]])
          selected-ids (get-selected-ids state table-id row->id)
          rows (mapv (fn [row]
                       (assoc row :selected? (contains? selected-ids (row->id row))))
                     rows)
          user-row->tr (into {} (map (fn [[thead f]]
                                       [thead
                                        (fn [row]
                                          (let [row (assoc row :selected? (contains? selected-ids (row->id row)))
                                                actions (f row)
                                                actions (walk/postwalk
                                                         (fn [x]
                                                           (case x
                                                             :data/query query
                                                             x))
                                                         actions)]
                                            actions))])
                                     row->tr))]
      (table-ui {:on-refresh [[:data/query query]
                               ;; some row may dissapear when refresh, so clear all selected ids
                              [:table/clear-selected-ids table-id]]
                 :on-new on-new
                 :theads theads
                 :table-id table-id
                 :multi-selection? multi-selection?
                 :select-all? select-all?
                 :rows rows
                 :row->tr user-row->tr
                 :row->id row->id
                 :page page
                 :size size
                 :go-page go-page
                 :set-size set-size}))))

(defn- text-filter [state filter-key label placeholder table-id]
  (let [value (get-in state [:tables table-id :filters filter-key])]
    [:div {:class ["form-control"]}
     [:label {:class ["label"]}
      [:span {:class ["label-text"]} label]]
     [:label {:class ["input"]}
      [:input {:type "text"
               :placeholder placeholder
               :value value
               :class ["grow" "font-mono" "text-sm"]
               :on {:input [[:store/assoc-in [:tables table-id :filters filter-key] :event/target.value]]}}]]]))

(defn- long-filter [state filter-key label placeholder table-id]
  (let [value (get-in state [:tables table-id :filters filter-key])]
    [:div {:class ["form-control"]}
     [:label {:class ["label"]}
      [:span {:class ["label-text"]} label]]
     [:label {:class ["input"]}
      [:input {:type "number"
               :placeholder placeholder
               :value value
               :class ["grow" "font-mono" "text-sm"]
               :on {:input [[:store/assoc-in [:tables table-id :filters filter-key] :event/target.value]]}}]]]))

(defn- range-filter [state filter-key label placeholder table-id]
  (let [min-value (get-in state [:tables table-id :filters (keyword (str (name filter-key) "-min"))])
        max-value (get-in state [:tables table-id :filters (keyword (str (name filter-key) "-max"))])]
    [:div {:class ["form-control"]}
     [:label {:class ["label"]}
      [:span {:class ["label-text"]} label]]
     [:div {:class ["flex" "gap-2"]}
      [:label {:class ["input"]}
       [:input {:type "number"
                :placeholder (str "Min " placeholder)
                :value min-value
                :class ["grow" "font-mono" "text-sm"]
                :on {:input [[:store/assoc-in [:tables table-id :filters (keyword (str (name filter-key) "-min"))] :event/target.value]]}}]]
      [:label {:class ["input"]}
       [:input {:type "number"
                :placeholder (str "Max " placeholder)
                :value max-value
                :class ["grow" "font-mono" "text-sm"]
                :on {:input [[:store/assoc-in [:tables table-id :filters (keyword (str (name filter-key) "-max"))] :event/target.value]]}}]]]]))

(defn- bool-filter [state filter-key label table-id]
  (let [_value (get-in state [:tables table-id :filters filter-key])]
    [:div {:class ["form-control"]}
     [:label {:class ["label"]}
      [:span {:class ["label-text"]} label]]
     [:label {:class ["input"]}
      [:select {:class ["select" "select-bordered" "w-full" "max-w-xs"]
                :on {:change [[:store/assoc-in
                               [:tables table-id :filters filter-key]
                               :event/target.value]]}}
       [:option {:value ""} "全选"]
       [:option {:value "true"} "是"]
       [:option {:value "false"} "否"]]]]))

(defn table-filter
  "filter-specs is list of map, each map contains :key :label :type and optionally :placeholder
   :type can be :text or :bool
   :placeholder is only needed for :text type"
  [state table-id filter-specs]
  [:div {:class ["mb-4" "p-4" "bg-base-100" "rounded-box" "border" "border-gray-200"]}
   [:div {:class ["flex" "items-center" "gap-4" "mb-3"]}
    [:h3 {:class ["text-lg" "font-semibold"]} "筛选条件"]]

   [:div {:class ["grid" "grid-cols-1" "md:grid-cols-2" "gap-4"]}
    (for [filter-spec filter-specs]
      (case (:type filter-spec)
        :text (text-filter state (:key filter-spec) (:label filter-spec) (:placeholder filter-spec) table-id)
        :bool (bool-filter state (:key filter-spec) (:label filter-spec) table-id)
        :long (long-filter state (:key filter-spec) (:label filter-spec) (:placeholder filter-spec) table-id)
        :range (range-filter state (:key filter-spec) (:label filter-spec) (:placeholder filter-spec) table-id)
        nil))]])

