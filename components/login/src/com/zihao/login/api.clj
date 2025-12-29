(ns com.zihao.login.api
  (:require
   [com.zihao.login.db :as db]
   [ring.util.response :as response]))

(defn command-handler [system {:command/keys [kind data]}] 
  (case kind
    :command/login
    (let [{:keys [username password]} data
          result (db/check-password username password)]
      (response/response {:success? result})) 

    nil))

(comment
  (command-handler nil
                   {:command/kind :command/login
                    :command/data {:username "admin"
                                   :password "PfeY3aXPdDdyeuBu"}})
  :rcf)

