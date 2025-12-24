(ns com.zihao.playground-jsonrpc.simple-server
  (:gen-class)
  (:require
   [jsonrpc4clj.io-server :as io-server]
   [jsonrpc4clj.server :as server]
   [clojure.java.io :as io])
  (:import
   [java.net ServerSocket InetAddress]))

(def log-file (io/file "simple_server.log"))

(defn log [& args]
  (with-open [w (io/writer log-file :append true)]
    (binding [*out* w]
      (apply println args)
      (.flush w))))

;; Simple greet method - matches the Python server example
;; jsonrpyc sends args as a positional array, which becomes a vector in Clojure
(defmethod server/receive-request "greet"
  [_ context params]
  ;; Debug: log what we received
  (log "Received greet request with params:" params)
  (log "Params type:" (type params))
  (log "Is vector?" (vector? params))
  (log "Context:" context)
  (let [name (if (vector? params)
               (first params)  ; Positional args come as vector
               params)]         ; Or as a single value
    (log "Extracted name:" name)
    (log "Returning:" (str "Hi, " name "!"))
    (str "Hi, " name "!")))

;; Catch-all default handler for any method not explicitly defined
(defmethod server/receive-request :default
  [method context params]
  (log "Received request for unknown method:" method)
  (log "Context:" context)
  (log "Params:" params)
  ;; Throw an exception that jsonrpc4clj will convert to a method not found error
  (throw (ex-info "Method not found"
                  {:code -32601
                   :message (str "Method not found: " method)
                   :data method})))

(defn -main [& args]
  (log "=== Server starting ===")
  (let [use-socket? (some #(= "--socket" %) args)
        port (when use-socket?
               (if-let [port-str (some->> args
                                          (drop-while #(not= "--port" %))
                                          second)]
                 (Long/valueOf port-str)
                 51234))]
    (if use-socket?
      ;; Socket server mode
      (do
        (log "Starting socket server on port:" port)
        (let [server-socket (ServerSocket. port 0 (InetAddress/getLoopbackAddress))
              _ (log "Socket bound, waiting for client connection...")
              client-socket (.accept server-socket)
              _ (log "Client connected!")
              server (io-server/server {:in client-socket
                                       :out client-socket
                                       :on-close #(do
                                                   (log "Closing client socket...")
                                                   (.close client-socket)
                                                   (log "Closing server socket...")
                                                   (.close server-socket))})]
          (log "Server created, starting...")
          (let [start-promise (server/start server {})]
            (log "Server started, blocking on server/start...")
            (let [result @start-promise]
              (log "Server shut down, result:" result)
              result))))
      ;; Stdio mode (default)
      (do
        (log "Starting stdio server...")
        (let [server (io-server/stdio-server)]
          (log "Server created, async thread should now be reading from stdin...")
          (Thread/sleep 50)
          (log "Starting server...")
          (let [start-promise (server/start server {})]
            (log "Server started, blocking on server/start, waiting for stdin to close...")
            (let [result @start-promise]
              (log "Server shut down, result:" result)
              result)))))))
