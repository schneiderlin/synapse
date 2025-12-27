(ns com.zihao.language-learn.router)

(def routes
  [[:pages/language-learn [["language-learn"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/language-learn nil
    nil))
