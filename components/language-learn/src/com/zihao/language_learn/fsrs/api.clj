(ns com.zihao.language-learn.fsrs.api
  (:require
   [datalevin.core :as d]
   [com.zihao.language-learn.fsrs.db :as db]
   [ring.util.response :as response]))

(defn get-due-cards []
  (let [ids (db/due-card-ids)]
    (d/pull-many (d/db db/conn) [:card/title :card/front :card/back
                                 :db/id] ids)))

(comment
  (get-due-cards)
  :rcf)

(defn query-handler [_system query]
  (case (:query/kind query)
    :query/due-cards
    (response/response {:success? true
                        :result (get-due-cards)}) 
    nil))

(comment
  (query-handler nil {:query/kind :query/due-cards}) 
  :rcf)

(defn command-handler [_system {:command/keys [kind data]}]
  (case kind 
    :command/repeat-card
    (let [{:keys [id rating]} data]
      (db/repeat-card! id rating)
      (response/response {:success? true
                          :result {:message "Card reviewed successfully"}}))

    nil))

(comment
  (command-handler nil {:command/kind :command/repeat-card,
                        :command/data {:id 1, :rating :good}})
  :rcf)