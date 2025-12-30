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

    :command/change-password
    (let [{:keys [username old-password new-password]} data
          result (db/change-password! username old-password new-password)]
      (response/response {:success? result}))

    nil))

(comment
  (command-handler nil
                   {:command/kind :command/login
                    :command/data {:username "admin"
                                   :password "PfeY3aXPdDdyeuBu"}})
  :rcf)

