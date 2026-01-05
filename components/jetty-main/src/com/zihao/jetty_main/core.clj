(ns com.zihao.jetty-main.core
  (:gen-class)
  (:require 
   [com.brunobonacci.mulog :as u] 
   [clojure.edn :as edn] 
   [reitit.ring :as ring]
   [ring.util.response :as response] 
   [ring.middleware.params :as params] 
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.cors :refer [wrap-cors]]
   [clojure.core.async :as async :refer [go-loop go <! <!!]]
   [muuntaja.middleware :as middleware]))

(defn html-handler [_request]
  (response/redirect "index.html"))

(defn make-http-query-handler [system query-handler]
  (fn [request]
    (let [query (:body-params request)
          result (query-handler system query)]
      (response/response {:success? true
                          :result result}))))

(defn make-ws-query-handler [query-handler]
  (fn [{:keys [event uid client-id ?data id ?reply-fn]}]
    (let [query ?data
          result (query-handler query)]
      (println "result" query result)
      (when ?reply-fn
        (?reply-fn result)))))

(defn make-http-command-handler [system command-handler]
  (fn [request]
    (let [command (:body-params request)
          result (command-handler system command)]
      (response/response {:success? true
                          :result result}))))

(defn make-ws-command-handler [command-handler]
  (fn [{:keys [event uid client-id ?data id ?reply-fn]}]
    (let [command ?data
          result (command-handler command)]
      (when ?reply-fn
        (?reply-fn result)))))

(defn upload-handler [request]
  (let [file (get-in request [:multipart-params "file"])
        payload (edn/read-string (get-in request [:multipart-params "payload"]))
        {:command/keys [kind data]} payload
        content (slurp (:tempfile file))]
    (case kind
      #_#_:command/upload-session
        (do
          (backend/add-session! backend/conn content)
          (response/response {:success? true})))))


(defn my-wrap-cors [handler]
  (wrap-cors handler :access-control-allow-origin [#"http://localhost:8000"]
             :access-control-allow-methods [:get :put :post :delete]))

(defn make-routes [system ws-server query-handler command-handler]
  (let [common-routes [["/" {:get html-handler
                             :post html-handler}]
                       ["/api" {:middleware [params/wrap-params
                                             middleware/wrap-format
                                             my-wrap-cors]}
                        ["/query" {:post (make-http-query-handler system query-handler)}]
                        ["/command" {:post (make-http-command-handler system command-handler)}]
                        ["/upload" {:middleware [wrap-multipart-params]
                                    :post upload-handler}]]]]
    (if ws-server
      (let [{:keys [ring-ajax-get-or-ws-handshake ring-ajax-post]} ws-server]
        (conj common-routes ["/chsk" {:middleware [params/wrap-params
                                                   wrap-keyword-params
                                                   wrap-session]
                                      :get ring-ajax-get-or-ws-handshake
                                      :post ring-ajax-post}]))
      common-routes)))

(defn make-ws-handler [query-handler command-handler]
  (fn [stop-ch {:keys [ch-chsk] :as ws-server}]
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg]
            (try
              (case id
                :chsk/ws-ping nil
                :test/echo (?reply-fn [id ?data])
                :data/query ((make-ws-query-handler query-handler) event-msg)
                :data/command ((make-ws-command-handler command-handler) event-msg)
                nil)
              (catch Exception e
                (u/log ::error :exception e))))
          (recur))))))

(defn make-handler [routes & {:keys [public-dir]}]
  (-> (ring/ring-handler
       (ring/router routes))
      (wrap-resource (or public-dir "public"))
      (wrap-content-type)))
