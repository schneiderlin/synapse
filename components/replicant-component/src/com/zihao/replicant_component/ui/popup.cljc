(ns com.zihao.replicant-component.ui.popup)

(defn popup [content & {:keys [on-cancel
                               on-confirm]}]
  [:div {:class ["fixed" "inset-0" "bg-gray-500/30" "flex" "items-center" "justify-center" "z-50"]}
   [:div {:class ["bg-white" "border" "border-gray-600" "rounded-lg"
                  "shadow-2xl" "min-w-64" "w-fit" "mx-4"]
          :style {:box-shadow "0 25px 50px -12px rgba(0, 0, 0, 0.75)"}}

    content

    [:div {:class ["flex"]}
     (when on-cancel
       [:button {:class ["btn" "btn-error" "btn-sm" "flex-1"]
                 :on {"click" on-cancel}}
        "取消"])
     (when on-confirm
       [:button {:class ["btn" "btn-success" "btn-sm" "flex-1"]
                 :on {"click" on-confirm}}
        "确认"])]]])

(defn popup-menu
  "on-click: a map with key as item, value as handler
   on-cancel: optional cancel handler"
  [items on-click & {:as opts}]
  (popup
   [:ul {:class ["py-2"]}
    (for [item items]
      [:li
       [:button {:class ["btn" "btn-ghost" "btn-sm" "w-full" "justify-start"]
                 :on {"click" (on-click item)}}
        item]])]
   opts))


