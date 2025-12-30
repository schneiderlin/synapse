(ns com.zihao.web-app.router
  (:require
   [domkm.silk :as silk]
   [com.zihao.playground-drawflow.router :as playground-drawflow-router]
   [com.zihao.language-learn.router :as language-learn-router]
   [com.zihao.xiangqi.router :as xiangqi-router]
   [com.zihao.login.router :as login-router]))

(def routes
  (silk/routes
   (concat
    playground-drawflow-router/routes
    language-learn-router/routes
    xiangqi-router/routes
    login-router/routes
    [[:pages/frontpage [["home"]]]])))

(defn get-location-load-actions [location]
  (or (playground-drawflow-router/get-location-load-actions location)
      (language-learn-router/get-location-load-actions location)
      (xiangqi-router/get-location-load-actions location)
      (login-router/get-location-load-actions location)))
