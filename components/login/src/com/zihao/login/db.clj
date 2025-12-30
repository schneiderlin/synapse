(ns com.zihao.login.db
  (:require 
   [datalevin.core :as d]))

(def db-uri "databases/login.db")
(def schema
  {:user/username {:db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}
   :user/password {:db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one}})

(def conn (d/get-conn db-uri schema))

(defn db-empty? []
  (let [query '[:find (count ?e) .
                :where [?e :user/username]]
        user-count (d/q query (d/db conn))]
    (zero? user-count)))

(defn add-user! [username password]
  (d/transact! conn [{:user/username username
                      :user/password password}]))

(defn ensure-initial-user! []
  (when (db-empty?)
    (add-user! "admin" "changeme")))

;; Create initial admin user on load
(ensure-initial-user!)

(comment
  (add-user! "admin" "PfeY3aXPdDdyeuBu") 
  :rcf)

(defn check-password [username password]
  (let [query '[:find ?e .
                :in $ ?username ?password
                :where
                [?e :user/username ?username]
                [?e :user/password ?password]]
        result (d/q query (d/db conn) username password)]
    (not (nil? result))))

(comment
  (check-password "admin" "PfeY3aXPdDdyeuBu")
  :rcf)

(defn user-exists? [username]
  (let [query '[:find ?e .
                :in $ ?username
                :where [?e :user/username ?username]]
        result (d/q query (d/db conn) username)]
    (not (nil? result))))

(defn change-password! [username old-password new-password]
  (if (check-password username old-password)
    (let [query '[:find ?e .
                  :in $ ?username
                  :where [?e :user/username ?username]]
          user-id (d/q query (d/db conn) username)]
      (d/transact! conn [[:db/add user-id :user/password new-password]])
      true)
    false))



