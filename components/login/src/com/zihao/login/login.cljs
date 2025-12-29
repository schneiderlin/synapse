(ns com.zihao.login.login
  (:require [com.zihao.replicant-main.replicant.command :as command]))

(defn login-form [state] 
  [:div {:class ["min-h-screen" "flex" "items-center" "justify-center" "bg-gray-50" "py-12" "px-4" "sm:px-6" "lg:px-8"]}
   [:div {:class ["max-w-md" "w-full" "space-y-8"]}
    [:div
     [:p {:class ["mt-2" "text-center" "text-sm" "text-gray-600"]}
      "请输入您的用户名和密码"]]

    ;; Error message display
    (when-let [login-command {:command/kind :command/login
                              :command/data {:username (get-in state [:login :username] "")
                                             :password (get-in state [:login :password] "")}}]
      (when (command/error? state login-command)
        [:div {:class ["bg-red-100" "border" "border-red-400" "text-red-700" "px-4" "py-3" "rounded"]}
         "用户名或密码错误"]))

    [:form {:class ["mt-8" "space-y-6"]
            :on {:submit [[:login/login {:username (get-in state [:login :username] "")
                                         :password (get-in state [:login :password] "")}]]}}

     [:div {:class ["rounded-md" "shadow-sm" "-space-y-px"]}
      [:div
       [:label {:for "username" :class ["sr-only"]} "用户名"]
       [:input {:id "username"
                :name "username"
                :type "text"
                :required true
                :class ["appearance-none" "rounded-none" "relative" "block" "w-full" "px-3" "py-2" "border" "border-gray-300" "placeholder-gray-500" "text-gray-900" "rounded-t-md" "focus:outline-none" "focus:ring-indigo-500" "focus:border-indigo-500" "focus:z-10" "sm:text-sm"]
                :placeholder "用户名"
                :value (get-in state [:login :username] "")
                :on {:input [[:store/assoc-in [:login :username] :event/target.value]]}}]]

      [:div
       [:label {:for "password" :class ["sr-only"]} "密码"]
       [:input {:id "password"
                :name "password"
                :type "password"
                :required true
                :class ["appearance-none" "rounded-none" "relative" "block" "w-full" "px-3" "py-2" "border" "border-gray-300" "placeholder-gray-500" "text-gray-900" "rounded-b-md" "focus:outline-none" "focus:ring-indigo-500" "focus:border-indigo-500" "focus:z-10" "sm:text-sm"]
                :placeholder "密码"
                :value (get-in state [:login :password] "")
                :on {:input [[:store/assoc-in [:login :password] :event/target.value]]}}]]]

     [:div
      [:button {:type "submit"
                :class ["group" "relative" "w-full" "flex" "justify-center" 
                        "py-2" "px-4" "border" "border-transparent" 
                        "text-sm" "font-medium" "rounded-md" 
                        "bg-indigo-600" "hover:bg-indigo-700" 
                        "focus:outline-none" "focus:ring-2" "focus:ring-offset-2" 
                        "focus:ring-indigo-500"]}
       "登录"]]]]])