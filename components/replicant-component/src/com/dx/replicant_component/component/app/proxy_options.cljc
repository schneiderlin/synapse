(ns com.zihao.replicant-component.component.app.proxy-options)

(defn proxy-options [state path] 
  (let [proxy-host (conj path :proxy-host)
        proxy-port (conj path :proxy-port)
        proxy-username (conj path :proxy-username)
        proxy-password (conj path :proxy-password)]
    [:div {:class ["space-y-4"]}
     [:span {:class ["text-sm" "font-medium" "text-gray-700"]} "代理配置"]

     ;; Proxy host
     [:label {:class ["floating-label" "w-full"]}
      [:span "代理主机"]
      [:input {:type "text" :placeholder "请输入代理主机地址" :class ["input" "input-md"]
               :value (get-in state proxy-host)
               :on {:input [[:store/assoc-in proxy-host :event/target.value]]}}]]

     ;; Proxy port
     [:label {:class ["floating-label" "w-full"]}
      [:span "代理端口"]
      [:input {:type "number" :placeholder "请输入代理端口" :class ["input" "input-md"]
               :value (get-in state proxy-port)
               :on {:input [[:store/assoc-in proxy-port :event/target.value]]}}]]

     ;; Proxy username
     [:label {:class ["floating-label" "w-full"]}
      [:span "代理用户名"]
      [:input {:type "text" :placeholder "请输入代理用户名（可选）" :class ["input" "input-md"]
               :value (get-in state proxy-username)
               :on {:input [[:store/assoc-in proxy-username :event/target.value]]}}]]

     ;; Proxy password
     [:label {:class ["floating-label" "w-full"]}
      [:span "代理密码"]
      [:input {:type "text" :placeholder "请输入代理密码（可选）" :class ["input" "input-md"]
               :value (get-in state proxy-password)
               :on {:input [[:store/assoc-in proxy-password :event/target.value]]}}]]]))
