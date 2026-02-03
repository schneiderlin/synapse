(ns com.zihao.llm-auth.actions)

(defn login [{:keys [username password]}]
  [[:event/prevent-default]
   [:data/command
    {:command/kind :llm-auth/login
     :command/data {:username username
                    :password password}}
    {:on-success [[:store/assoc-in [:llm-auth :authenticated?] true]
                  [:router/navigate {:location/page-id :llm-eval/dashboard}]]
     :on-failure [[:store/assoc-in [:llm-auth :error?] true]
                  [:store/assoc-in [:llm-auth :message] "Invalid username or password"]]}]])

(defn logout []
  [[:data/command
    {:command/kind :llm-auth/logout
     :command/data {}}
    {:on-success [[:store/assoc-in [:llm-auth :authenticated?] false]
                  [:store/assoc-in [:llm-auth :user] nil]
                  [:store/assoc-in [:llm-auth :permissions] nil]
                  [:router/navigate {:location/page-id :pages/frontpage}]]}]])

(defn update-language [{:keys [user-id language]}]
  [[:data/command
    {:command/kind :llm-auth/update-language
     :command/data {:user-id user-id
                    :language language}}
    {:on-success [[:store/assoc-in [:llm-auth :user :preferred-language] language]
                  [:store/assoc-in [:llm-auth :message] "Language updated successfully"]]
     :on-failure [[:store/assoc-in [:llm-auth :error?] true]
                  [:store/assoc-in [:llm-auth :message] "Failed to update language"]]}]])

(defn update-role [{:keys [user-id role]}]
  [[:data/command
    {:command/kind :llm-auth/update-role
     :command/data {:user-id user-id
                    :role role}}
    {:on-success [[:store/assoc-in [:llm-auth :user :role] role]
                  [:store/assoc-in [:llm-auth :message] "Role updated successfully"]]
     :on-failure [[:store/assoc-in [:llm-auth :error?] true]
                  [:store/assoc-in [:llm-auth :message] "Failed to update role"]]}]])

(defn execute-action [{:keys [_store] :as _system} _event action args]
  (case action
    :llm-auth/login (login args)
    :llm-auth/logout (logout)
    :llm-auth/update-language (update-language args)
    :llm-auth/update-role (update-role args)
    nil))

(comment
  (login {:username "admin" :password "admin"})
  (logout)
  (update-language {:user-id "user-001" :language "zh"})
  (update-role {:user-id "user-002" :role :admin})
  :rcf)
