(ns com.zihao.language-learn.router
  (:require
   [com.zihao.language-learn.lingq.router :as lingq-router]))

(def routes
  (concat
   [[:pages/language-learn [["language-learn"]]]]
   lingq-router/routes))

(defn get-location-load-actions [location]
  (or (case (:location/page-id location)
        :pages/language-learn nil
        nil)
      (lingq-router/get-location-load-actions location)))
