(ns com.dx.replicant-component.ui.tabs)

(defn tabs
  [& children]
  [:div {:class ["tabs" "tabs-border" "w-full"]}
   children])

(defn tab [name & children]
  (list
   [:input
    {:type "radio"
     :name name
     :class "tab"
     :aria-label name}]
   [:div {:class ["tab-content" "border-base-300" "bg-base-100" "p-6"]}
    children]))