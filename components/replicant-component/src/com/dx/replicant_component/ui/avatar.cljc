(ns com.zihao.replicant-component.ui.avatar)

(defn base64-avatar [base64-data]
  (if base64-data
    [:img {:src (str "data:image/png;base64," base64-data)
           :class ["w-12" "h-12" "rounded-full" "object-cover"]
           :alt "Profile Picture"}]
    [:div {:class ["w-12" "h-12" "rounded-full" "bg-gray-200" "flex" "items-center" "justify-center"]}
     [:span {:class ["text-gray-400" "text-xs"]} "无头像"]]))
