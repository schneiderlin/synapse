(ns com.zihao.jetty-main.core
  (:gen-class)
  (:require 
   [com.brunobonacci.mulog :as u] 
   [com.zihao.jetty-main.ring-ws :as ring-ws]
   [clojure.edn :as edn] 
   [cheshire.core :as json]
   [reitit.ring :as ring]
   [ring.util.response :as response] 
   [ring.middleware.params :as params] 
   [ring.middleware.content-type :refer [wrap-content-type]]
   [ring.middleware.resource :refer [wrap-resource]]
   [ring.middleware.keyword-params :refer [wrap-keyword-params]]
   [ring.middleware.session :refer [wrap-session]]
   [ring.middleware.multipart-params :refer [wrap-multipart-params]]
   [ring.middleware.cors :refer [wrap-cors]]
   [ring.websocket :as ws]
   [clojure.core.async :as async :refer [go-loop]]
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

(defn- ws-server-type
  "Detect the type of WebSocket server."
  [ws-server]
  (cond
    (nil? ws-server) nil
    (:chsk-send! ws-server) :sente
    (:sockets ws-server) :ring-ws
    ;; Legacy check for Sente
    (:ring-ajax-get-or-ws-handshake ws-server) :sente
    :else nil))

(defn make-routes 
  "Creates routes for the web application.
   
   Supports both Sente and Ring WebSocket servers:
   - Sente: adds /chsk endpoint
   - Ring WS: adds /ws endpoint
   
   Options:
   - :ws-path - custom path for Ring WS (default: \"/ws\")"
  [system ws-server query-handler command-handler & {:keys [ws-path] :or {ws-path "/ws"}}]
  (let [common-routes [["/" {:get html-handler
                             :post html-handler}]
                       ["/api" {:middleware [params/wrap-params
                                             middleware/wrap-format
                                             my-wrap-cors]}
                        ["/query" {:post (make-http-query-handler system query-handler)}]
                        ["/command" {:post (make-http-command-handler system command-handler)}]
                        ["/upload" {:middleware [wrap-multipart-params]
                                    :post upload-handler}]]]]
    (case (ws-server-type ws-server)
      :sente
      (let [{:keys [ring-ajax-get-or-ws-handshake ring-ajax-post]} ws-server]
        (conj common-routes ["/chsk" {:middleware [params/wrap-params
                                                   wrap-keyword-params
                                                   wrap-session]
                                      :get ring-ajax-get-or-ws-handshake
                                      :post ring-ajax-post}]))
      
      :ring-ws
      (conj common-routes [ws-path {:get (ring-ws/make-ring-ws-handler ws-server)
                                    :post (ring-ws/make-ring-ws-handler ws-server)}])
      
      ;; No WebSocket server
      common-routes)))

(defn make-ws-handler-with-extensions
  "Creates a WebSocket event handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [system event-msg] and return non-nil if handled.
   System map includes: :chsk-send!, :connected-uids, :ws-server"
  [& extension-fns]
  (fn [stop-ch {:keys [ch-chsk chsk-send! connected-uids] :as ws-server}]
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg]
            (try
              ;; Try extension functions first
              (or (some #(when-let [result (% {:chsk-send! chsk-send!
                                               :connected-uids connected-uids
                                               :ws-server ws-server}
                                              event-msg)]
                           result)
                       extension-fns)
                  ;; Fallback to built-in events
                  (case id
                    :chsk/ws-ping nil
                    :test/echo (?reply-fn [id ?data])
                    nil))
              (catch Exception e
                (u/log ::error :exception e))))
          (recur))))))

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

;; =============================================================================
;; Unified WebSocket Abstraction
;; =============================================================================

(defn make-sente-adapter
  "Creates a unified WebSocket context from a Sente server.
   Returns a map with:
   - :send! (fn [client-id event data]) - send to specific client
   - :broadcast! (fn [event data]) - send to all clients
   - :clients - deref-able collection of connected client IDs
   - :ch-recv - channel for incoming messages
   - :type - :sente"
  [{:keys [chsk-send! connected-uids ch-chsk] :as sente-server}]
  {:send! (fn [client-id event data]
            (chsk-send! client-id [event data]))
   :broadcast! (fn [event data]
                 (doseq [uid (:any @connected-uids)]
                   (chsk-send! uid [event data])))
   :clients connected-uids
   :ch-recv ch-chsk
   :type :sente
   :raw sente-server})

(defn- serialize-data
  "Serialize data according to format. Supports :edn and :json."
  [data format]
  (case format
    :json (json/generate-string data)
    :edn (pr-str data)
    ;; default to edn
    (pr-str data)))

(defn make-ring-ws-adapter
  "Creates a unified WebSocket context from a Ring WebSocket server.
   Returns a map with:
   - :send! (fn [client-id event data]) - send to specific client
   - :broadcast! (fn [event data]) - send to all clients
   - :clients - deref-able collection of connected client IDs
   - :ch-recv - channel for incoming messages
   - :type - :ring-ws
   - :format - serialization format (:edn or :json)
   
   The input map can include:
   - :format - :edn (default) or :json for message serialization"
  [{:keys [sockets ch-recv format] :or {format :edn} :as ring-ws-server}]
  (println "[adapter] Creating adapter with format:" format "sockets atom:" (System/identityHashCode sockets))
  {:send! (fn [client-id event data]
            (ring-ws/send! ring-ws-server client-id event data))
   :broadcast! (fn [event data]
                 (ring-ws/broadcast! ring-ws-server event data))
   :clients (reify clojure.lang.IDeref
              (deref [_] 
                (println "[adapter] Checking clients, sockets atom:" (System/identityHashCode sockets) "value:" @sockets)
                {:any (ring-ws/connected-clients ring-ws-server)}))
   :ch-recv ch-recv
   :type :ring-ws
   :format format
   :raw ring-ws-server})

(defn- normalize-message
  "Normalize an incoming message to unified format.
   
   Unified message format:
   {:event    keyword    - event identifier
    :data     any        - message payload
    :client-id string    - sender ID (may be nil for Sente)
    :reply!   fn         - (fn [data]) reply to this message}"
  [ws-ctx event-msg]
  (let [{:keys [id ?data ?reply-fn client-id socket]} event-msg
        ws-type (:type ws-ctx)
        format (get ws-ctx :format :edn)]
    {:event id
     :data ?data
     :client-id client-id
     :reply! (case ws-type
               :sente (fn [data]
                        (when ?reply-fn
                          (?reply-fn data)))
               :ring-ws (fn [data]
                          (when socket
                            (try
                              (ws/send socket (serialize-data {:event :reply :data data} format))
                              (catch Exception _ nil))))
               (fn [_] nil))}))

(defn make-unified-ws-handler
  "Creates a unified WebSocket event handler.
   
   Arguments:
   - handler-fn: (fn [ws-ctx message]) - your business logic handler
     - ws-ctx has: :send!, :broadcast!, :clients
     - message has: :event, :data, :client-id, :reply!
   
   Returns a function (fn [stop-ch ws-adapter]) that starts the event loop.
   
   Example:
   ```
   (defn my-handler [ws-ctx msg]
     (case (:event msg)
       :user/ping ((:reply! msg) {:pong true})
       :user/chat ((:broadcast! ws-ctx) :chat/message (:data msg))
       nil))
   
   (def handler (make-unified-ws-handler my-handler))
   (handler stop-ch (make-ring-ws-adapter ws-server))
   ```"
  [handler-fn]
  (fn [stop-ch ws-adapter]
    (let [{:keys [ch-recv type]} ws-adapter]
      (go-loop []
        (let [[event-msg port] (async/alts! [ch-recv stop-ch] :priority true)]
          (when (= port ch-recv)
            (let [{:keys [id client-id error]} event-msg]
              (try
                (case id
                  ;; Lifecycle events - log them
                  :ws/open (u/log ::ws-client-connected :client-id client-id)
                  :ws/close (u/log ::ws-client-disconnected :client-id client-id)
                  :ws/error (u/log ::ws-client-error :client-id client-id :error error)
                  :ws/parse-error (u/log ::ws-parse-error :client-id client-id :error error)
                  
                  ;; Internal pings - ignore silently
                  :chsk/ws-ping nil
                  
                  ;; Business events - pass to handler
                  (let [normalized-msg (normalize-message ws-adapter event-msg)]
                    (handler-fn ws-adapter normalized-msg)))
                (catch Exception e
                  (u/log ::handler-error :exception e))))
            (recur)))))))

(defn ws-adapter
  "Auto-detect and create the appropriate WebSocket adapter.
   Works with both Sente servers and Ring WebSocket servers."
  [ws-server]
  (cond
    ;; Sente server has these keys
    (:chsk-send! ws-server) (make-sente-adapter ws-server)
    ;; Ring WS server has :sockets atom
    (:sockets ws-server) (make-ring-ws-adapter ws-server)
    :else (throw (ex-info "Unknown WebSocket server type" {:server ws-server}))))
