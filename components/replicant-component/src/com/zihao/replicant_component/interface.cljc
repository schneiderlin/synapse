(ns com.zihao.replicant-component.interface
  (:require
   [com.zihao.replicant-component.component.table :as table]
   [com.zihao.replicant-component.ui.popup :as popup]))

(defn execute-action
  "Execute table-related actions in the system"
  [system event action args]
  (table/execute-action system event action args))

(defn table-filter
  "Render table filter UI components based on filter specifications"
  [state table-id filter-specs]
  (table/table-filter state table-id filter-specs))

(defn get-selected-rows
  "Get the set of selected rows for a given table"
  [state table-id]
  (table/get-selected-rows state table-id))

(defn get-selected-ids
  "Get the set of selected IDs for a given table using row->id function"
  [state table-id row->id]
  (table/get-selected-ids state table-id row->id))

(defn table-component
  "Render a table component with pagination, filtering, and selection capabilities"
  [query-kind theads row->tr & {:keys [on-new row->id table-id multi-selection? select-all?]
                                :or {table-id query-kind row->id identity}
                                :as opts}]
  (table/table-component query-kind theads row->tr opts))

(defn get-page
  "Get the current page number for a table"
  [state table-id]
  (table/get-page state table-id))

(defn get-size
  "Get the page size for a table"
  [state table-id]
  (table/get-size state table-id))

(defn get-filter
  "Get the current filter values for a table"
  [state table-id]
  (table/get-filter state table-id))

(defn get-rows
  "Get the rows for a table based on query kind and current state"
  [state table-id query-kind]
  (table/get-rows state table-id query-kind))

(defn popup
  "Render a modal popup with optional cancel and confirm buttons"
  [content & {:keys [on-cancel on-confirm]}]
  (popup/popup content :on-cancel on-cancel :on-confirm on-confirm))

(defn popup-menu
  "Render a popup menu with clickable items
   items: list of menu items
   on-click: function that takes an item and returns a handler
   opts: optional map with :on-cancel and other popup options"
  [items on-click & {:as opts}]
  (popup/popup-menu items on-click opts))

