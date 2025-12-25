(ns com.zihao.web-app.router
  (:require
   [domkm.silk :as silk]
   [com.zihao.playground-drawflow.router :as playground-drawflow-router]))

(def routes
  (silk/routes
   (concat
    playground-drawflow-router/routes
    [[:pages/frontpage [["home"]]]])))

(defn get-location-load-actions [location] 
  (or  
      (playground-drawflow-router/get-location-load-actions location)
      ))
