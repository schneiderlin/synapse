(ns com.zihao.language-learn.fsrs.router
  (:require
   [com.zihao.replicant-main.replicant.query :as query]))

(def routes
  [[:pages/fsrs [["fsrs"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/fsrs [[:data/query {:query/kind :query/due-cards}]]
    nil))
