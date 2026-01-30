(ns com.zihao.llm-auth.interface
  (:require
   #?(:clj [com.zihao.llm-auth.api :as api])
   #?(:clj [com.zihao.llm-auth.db :as db])
   [com.zihao.llm-auth.permissions :as perms]))

(defn prefix
  "The prefix key used to access llm-auth state in a nested state map"
  :llm-auth)

(defn query-handler
  "Query handler for llm-auth component"
  [system query]
  #?(:clj (api/query-handler system query)
     :cljs (throw (ex-info "query-handler only available in Clojure" {}))))

(defn command-handler
  "Command handler for llm-auth component"
  [system command]
  #?(:clj (api/command-handler system command)
     :cljs (throw (ex-info "command-handler only available in Clojure" {}))))

(defn has-permission?
  "Check if a role has a specific permission"
  [role permission]
  (perms/has-permission? role permission))

(defn get-navigation-for-role
  "Get navigation items accessible to a role"
  [role]
  (perms/get-navigation-for-role role))

(defn get-permissions-for-role
  "Get all permissions for a role"
  [role]
  (perms/get-permissions-for-role role))

(defn roles
  "Get all available roles"
  []
  perms/roles)

#?(:clj
   (defn check-credentials
     "Check user credentials and return user data if valid"
     [username password]
     (db/check-credentials username password)))

#?(:clj
   (defn get-user-by-username
     "Get user by username"
     [username]
     (db/get-user-by-username username)))

#?(:clj
   (defn get-user-by-id
     "Get user by ID"
     [user-id]
     (db/get-user-by-id user-id)))
