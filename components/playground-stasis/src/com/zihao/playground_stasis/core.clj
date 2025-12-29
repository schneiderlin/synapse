(ns com.zihao.playground-stasis.core
  (:require
   [stasis.core :as stasis] 
   [ring.adapter.jetty :as jetty]))

(defn pages []
  {"/index.html" "<h1>Welcome!</h1>"
   "/test.html" "<h1>Test!</h1>"})

;; app 就是一个 ring handler
(def app (stasis/serve-pages pages))

;; 启动服务器
(defn start-server [port]
  (jetty/run-jetty #'app {:port port :join? false}))

(comment 
  ;; 测试 stasis handler
  (app {:request-method :get :uri "/index.html"})
  (app {:request-method :get :uri "/test.html"})
  
  ;; 启动服务器
  (def server (start-server 3000))
  
  ;; 停止服务器
  (.stop server)
  :rcf)
