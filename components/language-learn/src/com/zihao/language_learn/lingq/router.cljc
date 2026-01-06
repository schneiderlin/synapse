(ns com.zihao.language-learn.lingq.router)

(def routes
  [[:pages/lingq [["lingq"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/lingq [[:data/query {:query/kind :query/get-word-rating}]]
    nil))
