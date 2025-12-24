(ns com.dx.replicant-main.replicant.navbar)

(defn link [page-id page-name children]
  (if (nil? children)
    ;; 普通
    [:li
     [:ui/a {:ui/location {:location/page-id page-id}}
      page-name]]
    ;; 嵌套
    [:li
     [:details
      {:open false}
      [:summary page-name]
      [:ul
       (for [{:keys [page-id page-name]} children]
         (link page-id page-name nil))]]]))

(defn navbar [title links]
  [:div {:class ["navbar" "bg-base-100" "shadow-sm" "mb-4"]}
   [:div {:class ["navbar-start"]}
    [:a {:class ["btn" "btn-ghost" "text-xl"]} title]]

   [:div {:class ["navbar-center" "hidden" "lg:flex"]}
    [:ul {:class ["menu" "menu-horizontal" "px-1"]}
     (for [{:keys [page-id page-name children]} links]
       (link page-id page-name children))]]])
