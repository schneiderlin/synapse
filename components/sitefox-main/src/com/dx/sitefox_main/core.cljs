(ns com.zihao.sitefox-main.core
  (:require
   [com.zihao.whatsapp-account.interface :as whatsapp-account]
   [promesa.core :as p] 
   [sitefox.web :as web]
   [sitefox.logging :refer [bind-console-to-file]]
   [clojure.edn :as edn]))

(bind-console-to-file)

(defonce server (atom nil))

(defn home-page [_req res] 
  (.send res "Hello world! yes"))

(defn query-handler [req res]
  (let [body-text (.-body req)
        query (edn/read-string body-text)
        result #_[1 2 3] (whatsapp-account/query-handler query)]
    (.set res "Content-Type" "application/edn")
    (.send res (pr-str result))))

(comment
 (let [query {:query/kind :query/whatsapp-accounts}] 
   (whatsapp-account/query-handler query)) 
  
  :rcf)



(defn command-handler [req res]
  (try
    (let [body-text (.-body req)
          command (edn/read-string body-text)
          result {:success? true
                  :result {:command command
                           :message "Command handler received your request"}}]
      (.set res "Content-Type" "application/edn")
      (.send res (pr-str result)))
    (catch js/Error e
      (.status res 400)
      (.set res "Content-Type" "application/edn")
      (.send res (pr-str {:success? false
                          :error (.-message e)})))))

(defn setup-routes [^js app] 
  (web/reset-routes app)
  (let [^js pre-csrf-router (aget app "pre-csrf-router")]
    (.get app "/" home-page)
    (.post pre-csrf-router "/api/query" query-handler)
    (.post pre-csrf-router "/api/command" command-handler)))

(defn main! []
  (set! (.-PORT js/process.env) "5000")
  (p/let [[app host port] (web/start)]
    (reset! server app)
    (setup-routes app)
    (println "Server listening on" (str "http://" host ":" port))))

(defn ^:dev/after-load reload []
  (js/console.log "reload")
  (setup-routes @server))
