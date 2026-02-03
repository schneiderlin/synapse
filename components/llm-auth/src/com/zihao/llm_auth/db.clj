(ns com.zihao.llm-auth.db
  (:require
   [datalevin.core :as d]))

(def db-uri "databases/llm-auth.db")

(def schema
  {:auth-user/id {:db/valueType :db.type/string
                  :db/cardinality :db.cardinality/one
                  :db/unique :db.unique/identity}
   :auth-user/username {:db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one
                        :db/unique :db.unique/identity}
   :auth-user/password {:db/valueType :db.type/string
                        :db/cardinality :db.cardinality/one}
   :auth-user/role {:db/valueType :db.type/string
                    :db/cardinality :db.cardinality/one}
   :auth-user/preferred-language {:db/valueType :db.type/string
                                  :db/cardinality :db.cardinality/one}})

(def conn (d/get-conn db-uri schema))

(defn db-empty? []
  (let [query '[:find ?e .
                :where [?e :auth-user/username]]
        result (d/q query (d/db conn))]
    (nil? result)))

(defn add-user! [username password role preferred-language]
  (d/transact! conn [{:auth-user/id (str (random-uuid))
                      :auth-user/username username
                      :auth-user/password password
                      :auth-user/role role
                      :auth-user/preferred-language preferred-language}]))

(defn get-user-by-username [username]
  (let [query '[:find (pull ?e [*]) .
                :in $ ?username
                :where [?e :auth-user/username ?username]]
        result (d/q query (d/db conn) username)]
    result))

(defn get-user-by-id [id]
  (let [query '[:find (pull ?e [*]) .
                :in $ ?id
                :where [?e :auth-user/id ?id]]
        result (d/q query (d/db conn) id)]
    result))

(defn check-credentials [username password]
  (let [user (get-user-by-username username)]
    (when (and user (= (:auth-user/password user) password))
      {:auth-user/id (:auth-user/id user)
       :auth-user/username (:auth-user/username user)
       :auth-user/role (:auth-user/role user)
       :auth-user/preferred-language (:auth-user/preferred-language user)})))

(defn update-user-language! [user-id language]
  (d/transact! conn [[:db/add user-id :auth-user/preferred-language language]]))

(defn update-user-role! [user-id role]
  (d/transact! conn [[:db/add user-id :auth-user/role role]]))

(defn ensure-initial-users! []
  (when (db-empty?)
    (add-user! "admin" "admin" "admin" "en")
    (add-user! "agent" "agent" "agent" "en")))

(defn seed-users! []
  (d/transact! conn [{:auth-user/id "user-001"
                      :auth-user/username "admin"
                      :auth-user/password "admin"
                      :auth-user/role "admin"
                      :auth-user/preferred-language "en"}
                     {:auth-user/id "user-002"
                      :auth-user/username "agent"
                      :auth-user/password "agent"
                      :auth-user/role "agent"
                      :auth-user/preferred-language "en"}]))

(defn init! 
  "Initialize database with default users if empty"
  []
  (when (db-empty?)
    (seed-users!)))

(comment
  (check-credentials "admin" "admin")
  (get-user-by-username "admin")
  (get-user-by-id "user-001")
  (update-user-language! "user-001" "zh")
  (update-user-role! "user-002" "admin")
  :rcf)
