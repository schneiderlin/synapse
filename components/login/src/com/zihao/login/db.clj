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

(defn add-user! [username password]
  (d/transact! conn [{:user/username username
                      :user/password password}]))

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



