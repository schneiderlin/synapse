(ns com.zihao.web-app.api
  (:require
   [ring.adapter.jetty :as jetty]
   [com.zihao.jetty-main.interface :as jm]
   [com.zihao.jetty-main.ws-server :as ws-server]
   [com.zihao.cljpy-main.interface :as cljpy-main]
   [com.zihao.language-learn.interface :as language-learn]
   [com.zihao.xiangqi.interface :as xiangqi]
   [com.zihao.llm-eval.interface :as llm-eval]
   [integrant.core :as ig]
   [clojure.core.async :as async]))

(defn command-handler [system command]
  (or (language-learn/command-handler system command)
      (xiangqi/command-handler system command)
      (llm-eval/command-handler system command)))

(defn query-handler [system query]
  (or (language-learn/query-handler system query)
      (xiangqi/query-handler system query)
      (llm-eval/query-handler system query)))

(defn ws-event-handler [system event-msg]
  (or (xiangqi/ws-event-handler system event-msg)))

(comment
  (query-handler nil {:query/kind :query/due-cards})
  :rcf)

(def config
  {:ws/ws-server true
   :ws/ws-handler {:ws-server (ig/ref :ws/ws-server)
                   :ws-event-handler (ig/ref :ws/event-handler)}
   :ws/event-handler []
   :jetty/routes {:cljpy/python-env (ig/ref :cljpy/python-env)
                  :ws-server (ig/ref :ws/ws-server)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}
   :cljpy/python-env {:built-in "builtins"
                      #_#_:u2 "uiautomator2"}})

(defmethod ig/init-key :cljpy/python-env [_ modules]
  (cljpy-main/make-python-env [] modules))

(defmethod ig/init-key :ws/ws-server [_ enabled?]
  (when enabled?
    (ws-server/make-ws-server)))

(defmethod ig/init-key :ws/event-handler [_ _]
  (fn [event-msg]
    (ws-event-handler nil event-msg)))

(defmethod ig/init-key :ws/ws-handler [_ {:keys [ws-server ws-event-handler]}]
  (when ws-server
    (let [stop-ch (async/chan)
          handler (jm/make-ws-handler-with-extensions ws-event-handler)]
      (handler stop-ch ws-server)
      stop-ch)))

(defmethod ig/halt-key! :ws/ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}]
  (jm/make-routes system ws-server query-handler command-handler))

(defmethod ig/init-key :jetty/handler [_ routes]
  (jm/make-handler routes :public-dir "public"))

(defmethod ig/init-key :adapter/jetty [_ {:keys [port handler]}]
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defn -main [& _]
  (ig/init config))

(comment
  (def system (-main))

  (def ws-server (:ws/ws-server system))
  (def send-fn (:chsk-send! ws-server))

  (require '[taoensso.sente :as sente])

  ;; server send
  (send-fn
   "uid" ;; user id 
   [:subscribe/event 1] ; Event
   8000 ; Timeout
   )

  ;; client send
  (send-fn
   [:test/echo {:name "Rich Hickey" :type "Awesome"}] ; Event
   8000 ; Timeout
   (fn [reply] ; Reply is arbitrary Clojure data 
     (when (sente/cb-success? reply)
       (println reply))))

  (def python-env (:cljpy/python-env system))
  :rcf)
