(ns com.zihao.login.router)

(def routes
  [[:pages/login [["login"]]]
   [:pages/change-password [["change-password"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/login []
    :pages/change-password []
    nil))

(comment
  (get-location-load-actions {:location/page-id :pages/login})
  :rcf)

