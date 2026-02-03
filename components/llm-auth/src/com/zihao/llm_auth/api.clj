(ns com.zihao.llm-auth.api
  (:require
   [com.zihao.llm-auth.db :as db]
   [com.zihao.llm-auth.permissions :as perms]))

(defn command-handler [_system {:command/keys [kind data]}]
  (case kind
    :llm-auth/login
    (let [{:keys [username password]} data
          user (db/check-credentials username password)]
      (when user
        {:success? true
         :user {:id (:auth-user/id user)
                :username (:auth-user/username user)
                :role (keyword (:auth-user/role user))
                :preferred-language (:auth-user/preferred-language user)}
         :permissions (perms/get-permissions-for-role (keyword (:auth-user/role user)))}))

    :llm-auth/logout
    {:success? true
     :message "Logged out successfully"}

    :llm-auth/update-language
    (let [{:keys [user-id language]} data]
      (db/update-user-language! user-id language)
      {:success? true})

    :llm-auth/update-role
    (let [{:keys [user-id role]} data]
      (when (perms/check-role role)
        (db/update-user-role! user-id (name role))
        {:success? true}))

    nil))

(defn query-handler [_system {:query/keys [kind data]}]
  (case kind
    :llm-auth/navigation
    (let [{:keys [role]} data]
      {:navigation (perms/get-navigation-for-role role)})

    :llm-auth/permissions
    (let [{:keys [role]} data]
      {:permissions (perms/get-permissions-for-role role)})

    :llm-auth/check-permission
    (let [{:keys [role permission]} data]
      {:has-permission? (perms/has-permission? role permission)})

    :llm-auth/roles
    {:roles perms/roles}

    nil))

(comment
  (command-handler nil
                   {:command/kind :llm-auth/login
                    :command/data {:username "admin"
                                   :password "admin"}})
  (command-handler nil
                   {:command/kind :llm-auth/logout
                    :command/data {}})
  (query-handler nil
                 {:query/kind :llm-auth/navigation
                  :query/data {:role :admin}})
  (query-handler nil
                 {:query/kind :llm-auth/permissions
                  :query/data {:role :agent}})
  :rcf)
