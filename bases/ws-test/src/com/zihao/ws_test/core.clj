(ns com.zihao.ws-test.core
  "Test server for the unified WebSocket abstraction.
   
   Start the server:
     (def server (start-server))
   
   Stop the server:
     (.stop server)
   
   Test with the Node.js client:
     node bases/ws-test/client.mjs"
  (:require
   [com.zihao.jetty-main.interface :as jm]
   [ring.adapter.jetty :as jetty]
   [clojure.core.async :as async]))

;; =============================================================================
;; Business Logic Handler (same code works with Sente or Ring WS!)
;; =============================================================================

(defn ws-handler
  "Example WebSocket message handler.
   
   Unified message format:
   {:event    keyword - the event type
    :data     any     - the payload
    :client-id string - who sent it  
    :reply!   fn      - call ((:reply! msg) data) to reply}"
  [ws-ctx msg]
  (let [{:keys [event data client-id reply!]} msg
        {:keys [send! broadcast! clients]} ws-ctx]
    (println "[WS] Received:" event "from" client-id "data:" data)
    
    (case event
      ;; Echo back to sender
      :test/echo
      (do
        (println "[WS] Echoing back to" client-id)
        (reply! {:echo data :from :server}))
      
      ;; Ping-pong
      :test/ping
      (do
        (println "[WS] Pong!")
        (reply! {:pong true :timestamp (System/currentTimeMillis)}))
      
      ;; Broadcast to all clients
      :test/broadcast
      (do
        (println "[WS] Broadcasting to all clients:" (keys (:any @clients)))
        (broadcast! :server/broadcast {:message data :from client-id}))
      
      ;; Send to specific client
      :test/send-to
      (let [{:keys [target-id message]} data]
        (println "[WS] Sending to" target-id ":" message)
        (send! target-id :server/message {:message message :from client-id}))
      
      ;; List connected clients
      :test/list-clients
      (do
        (println "[WS] Connected clients:" (:any @clients))
        (reply! {:clients (vec (:any @clients))}))
      
      ;; Default - unknown event
      (println "[WS] Unknown event:" event))))

;; =============================================================================
;; HTTP Handlers (minimal, just for testing)
;; =============================================================================

(defn query-handler [_system query]
  {:query query :response "ok"})

(defn command-handler [_system command]
  {:command command :response "ok"})

;; =============================================================================
;; Server Setup
;; =============================================================================

(defonce !ws-server (atom nil))
(defonce !ws-adapter (atom nil))
(defonce !stop-ch (atom nil))

(defn start-server
  "Start the test WebSocket server on port 3333.
   
   Returns the Jetty server instance."
  [& {:keys [port] :or {port 3333}}]
  ;; Create Ring WebSocket server
  (let [ws-server (jm/make-ring-ws-server)
        _ (reset! !ws-server ws-server)
        
        ;; Create adapter and start handler
        adapter (jm/ws-adapter ws-server)
        _ (reset! !ws-adapter adapter)
        
        stop-ch (async/chan)
        _ (reset! !stop-ch stop-ch)
        
        ;; Start the unified handler
        handler (jm/make-unified-ws-handler ws-handler)
        _ (handler stop-ch adapter)
        
        ;; Create routes with Ring WS support
        routes (jm/make-routes {} ws-server query-handler command-handler)
        ring-handler (jm/make-handler routes :public-dir "ws-test")]
    
    (println "")
    (println "=================================================")
    (println "  WebSocket Test Server Starting on port" port)
    (println "=================================================")
    (println "")
    (println "WebSocket endpoint: ws://localhost:" port "/ws")
    (println "")
    (println "Test with Node.js client:")
    (println "  node bases/ws-test/client.mjs")
    (println "")
    
    ;; Start Jetty
    (jetty/run-jetty ring-handler {:port port :join? false})))

(defn stop-server [server]
  "Stop the server and cleanup."
  (when @!stop-ch
    (async/put! @!stop-ch :stop))
  (.stop server)
  (println "Server stopped."))

;; =============================================================================
;; REPL Helpers
;; =============================================================================

(comment
  ;; Start the server
  (def server (start-server))
  
  ;; Check connected clients
  @(:clients @!ws-adapter)
  
  ;; Broadcast a message from REPL
  ((:broadcast! @!ws-adapter) :server/announcement {:msg "Hello from REPL!"})
  
  ;; Stop the server
  (stop-server server)
  
  :rcf)
