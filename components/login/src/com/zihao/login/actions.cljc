(ns com.zihao.login.actions
  (:require
   [clojure.string :as str]))

(defn login [{:keys [username password]}]
  [[:event/prevent-default]
   [:data/command
    {:command/kind :command/login
     :command/data {:username username
                    :password password}}
    {:on-success [[:store/assoc-in [:login :success?] true]
                  [:router/navigate {:location/page-id :pages/frontpage}]]}]])

(defn change-password [{:keys [username old-password new-password]}]
  [[:event/prevent-default]
   [:data/command
    {:command/kind :command/change-password
     :command/data {:username username
                    :old-password old-password
                    :new-password new-password}}
    {:on-success [[:store/assoc-in [:change-password :success?] true]
                  [:store/assoc-in [:change-password :message] "密码修改成功"]]
     :on-failure [[:store/assoc-in [:change-password :error?] true]
                  [:store/assoc-in [:change-password :message] "旧密码错误"]]}]])

(defn execute-action [{:keys [store] :as system} event action args]
  (case action
    :login/login (login args)
    :login/change-password (change-password args)
    nil))