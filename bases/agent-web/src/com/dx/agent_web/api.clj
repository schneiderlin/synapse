(ns com.dx.agent-web.api
  (:require
   [ring.adapter.jetty :as jetty]
   [com.dx.jetty-main.interface :as jm]
   [com.dx.jetty-main.ws-server :as ws-server]
   [com.dx.agent.chat-agent :as agent]
   [com.dx.cljpy-main.interface :as cljpy-main]
   [com.dx.agent.code-executor :refer [code-executor]]
   [clojure.core.async :as async :refer [go-loop]]
   [taoensso.telemere :as tel]
   [integrant.core :as ig]))

(defn command-handler [_ _]
  nil)

(defn query-handler [_]
  nil)

(defn make-ws-handler [{:keys [ws/ws-server agent/store cljpy/python-env]
                        :as system}]
  (fn [stop-ch {:keys [ch-chsk chsk-send! connected-uids]}]
    (let [ws-command-handler (fn [{:keys [?data ?reply-fn]}]
                               (let [command ?data
                                     result (command-handler nil command)]
                                 (when ?reply-fn
                                   (?reply-fn result))))
          ws-query-handler (fn [{:keys [?data ?reply-fn]}]
                             (let [query ?data
                                   result (query-handler query)]
                               (when ?reply-fn
                                 (?reply-fn result))))
          ;; Helper to send events to all connected clients
          send-to-all (fn [event data]
                        (doseq [uid (:any @connected-uids)]
                          (chsk-send! uid [event data])))
          ;; WebSocket app that sends messages to frontend
          websocket-app {:add-log (fn [s] (println s)) ; Keep logging to console
                         :add-assistant-output (fn [s]
                                                 (send-to-all :agent/partial-result {:text s}))
                         :add-clojure-code (fn [s]
                                             (send-to-all :agent/partial-result {:text (str "```clojure\n" s "\n```")}))
                         :add-plain-text (fn [s]
                                           (send-to-all :agent/partial-result {:text s}))
                         :finish-streaming-output (fn []
                                                    (send-to-all :agent/response-end {}))}
          agent-ctx {:store store
                     :app websocket-app
                     :code-executor code-executor}]
      (go-loop []
        (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
          (when (= port ch-chsk)
            (let [{:keys [?data id ?reply-fn]} event-msg]
              (try
                (case id
                  :chsk/ws-ping nil
                  :test/echo (when ?reply-fn (?reply-fn [id ?data]))
                  :data/query (ws-query-handler event-msg)
                  :data/command (ws-command-handler event-msg)
                  :ai/send-user-message (let [{:keys [content]} ?data]
                                          (println "[WS] Received user message:" content)
                                          ;; Add user message to agent store
                                          (swap! store (fn [state]
                                                         (update (or state {:msgs []}) :msgs conj
                                                                 {:role "user"
                                                                  :content content})))
                                          ;; Trigger agent loop in background
                                          (async/go
                                            (loop [step 0]
                                              (when (< step 20) ; max steps
                                                (let [actions (agent/get-actions agent-ctx)]
                                                  (if (empty? actions)
                                                    (do
                                                      (println "[Agent] No more actions")
                                                      (send-to-all :agent/response-end {}))
                                                    (do
                                                      (agent/execute-actions system agent-ctx actions)
                                                      (async/<! (async/timeout 100))
                                                      (recur (inc step))))))))
                                          (when ?reply-fn
                                            (?reply-fn {:status :received})))
                  nil)
                (catch Exception e
                  (tel/error! e))))
            (recur)))))))

(def config
  {:agent/store nil
   :ws/ws-server nil
   :ws/ws-handler {:ws-server (ig/ref :ws/ws-server)
                   :agent/store (ig/ref :agent/store)
                   :cljpy/python-env (ig/ref :cljpy/python-env)}
   :cljpy/python-env nil
   :jetty/routes {:ws-server (ig/ref :ws/ws-server)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}})

(defmethod ig/init-key :cljpy/python-env [_ _]
  (cljpy-main/make-python-env ["./components/baml-client"]
                              {:built-in "builtins"
                               :sys "sys"}))

(defmethod ig/init-key :agent/store [_ _]
  (atom {:msgs []}))

(defmethod ig/init-key :ws/ws-server [_ _]
  (ws-server/make-ws-server))

(defmethod ig/init-key :ws/ws-handler [_ {:keys [ws-server cljpy/python-env] :as config}] 
  (let [stop-ch (async/chan)
        agent-store (:agent/store config)
        handler (make-ws-handler {:ws/ws-server ws-server
                                  :agent/store agent-store
                                  :cljpy/python-env python-env})]
    (handler stop-ch ws-server)
    stop-ch))

(comment
  ;; stop and restart ws-handler when code changes
  (def ws-handler (:ws/ws-handler @!system))
  (async/put! ws-handler :stop)

  ;; TODO: 需要用 integrant 的 suspend 和 resume, 注册多两个 multi method
  ;; 现在创建 ws-handler 的代码还需要提取出公共部分
  :rcf)

(defmethod ig/halt-key! :ws/ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}] 
  (jm/make-routes system ws-server query-handler command-handler))

(defmethod ig/init-key :jetty/handler [_ routes]
  (jm/make-handler routes :public-dir "bases/agent-web/resources/public"))

(defmethod ig/init-key :adapter/jetty [_ {:keys [port handler]}] 
  (jetty/run-jetty handler {:port port :join? false}))

(defmethod ig/halt-key! :adapter/jetty [_ server]
  (.stop server))

(defn -main [& _]
  (ig/init config))

(comment
  (def !system (atom nil))
  (reset! !system (-main))

  (def ws-server (:ws/ws-server @!system))
  (def connected-uids (:connected-uids ws-server))
  (:any @connected-uids)

  ;; 看 agent memory
  (:agent/store @!system)

  ;; Fake agent functions for testing
  (defn fake-agent-send-text [system text]
    ;; Send text chunk to frontend via WebSocket
    (let [{:keys [ws/ws-server]} system
          {:keys [chsk-send! connected-uids]} ws-server]
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid [:agent/emit-text {:text text}]))))
  
  (fake-agent-send-text @!system "Hello")
  (fake-agent-send-text @!system " World!")

  (defn fake-agent-send-response-end [system]
    ;; Signal response end to frontend
    (let [{:keys [ws/ws-server]} system
          {:keys [chsk-send! connected-uids]} ws-server]
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid [:agent/response-end {}]))))
  
  (fake-agent-send-response-end @!system)

  (defn fake-agent-send-start-tool-call [system tool-name]
    ;; Signal start of tool call
    (let [{:keys [ws/ws-server]} system
          {:keys [chsk-send! connected-uids]} ws-server]
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid [:agent/start-tool-call {:tool-name tool-name}]))))
  
  (fake-agent-send-start-tool-call @!system "tool-name")

  (defn fake-agent-send-end-tool-call [system]
    ;; Signal end of tool call
    (let [{:keys [ws/ws-server]} system
          {:keys [chsk-send! connected-uids]} ws-server]
      (doseq [uid (:any @connected-uids)]
        (chsk-send! uid [:agent/end-tool-call {}]))))

  (fake-agent-send-end-tool-call @!system) 
  :rcf)
