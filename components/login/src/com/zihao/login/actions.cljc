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

(defn execute-action [{:keys [store] :as system} event action args] 
  (case action
    :login/login (login args)
    nil))