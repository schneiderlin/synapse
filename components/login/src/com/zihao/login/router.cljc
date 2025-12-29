(ns com.zihao.login.router)

(def routes
  [[:pages/login [["login"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/login []
    nil))

(comment
  (get-location-load-actions {:location/page-id :pages/login})
  :rcf)

