(ns com.zihao.language-learn.lingq.router
  (:require
   [com.zihao.replicant-main.replicant.query :as query]))

(def routes
  [[:pages/lingq [["lingq"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/lingq [[:data/query {:query/kind :query/get-word-rating}]]
    nil))
