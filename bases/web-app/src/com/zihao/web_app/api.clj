(ns com.zihao.web-app.api
  (:require
   [ring.adapter.jetty :as jetty]
   [com.zihao.jetty-main.interface :as jm] 
   [com.zihao.cljpy-main.interface :as cljpy-main]
   [integrant.core :as ig]))

(defn command-handler [system command] 
  (or  
      #_(script-config/command-handler command)
      ))

(defn query-handler [query]
  (or  
      #_(script-config/query-handler query)
      ))

(def config
  {:jetty/routes {:ws-server nil
                  :cljpy/python-env (ig/ref :cljpy/python-env)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}
   :cljpy/python-env {:built-in "builtins"
                      #_#_:u2 "uiautomator2"}})

(defmethod ig/init-key :cljpy/python-env [_ modules]
  (cljpy-main/make-python-env [] modules))

(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}] 
  (jm/make-routes system ws-server query-handler command-handler))

(defmethod ig/init-key :jetty/handler [_ routes]
  (jm/make-handler routes :public-dir "bases/web-app/resources/public"))

(defmethod ig/init-key :adapter/jetty [_ {:keys [port handler]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defn -main [& _]
  (ig/init config))

(comment
  (def system (-main))

  (def python-env (:cljpy/python-env system))
  :rcf)
