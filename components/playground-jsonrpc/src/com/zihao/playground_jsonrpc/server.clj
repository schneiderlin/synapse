(ns com.zihao.playground-jsonrpc.server
  (:gen-class)
  (:require
   [com.brunobonacci.mulog :as u]
   [clojure.core.async :as async]
   [jsonrpc4clj.coercer :as coercer]
   [jsonrpc4clj.io-server :as io-server]
   [jsonrpc4clj.server :as server]))

;; a notification; return value is ignored
(defmethod server/receive-notification "textDocument/didOpen"
  [_ context {:keys [text-document]}]
  (u/log ::log :data {:context context :text-document text-document}))

;; a request; return value is converted to a response
(defmethod server/receive-request "textDocument/definition"
  [_ context params]
  (u/log ::log :data {:context context :params params})
  (::coercer/location params))

(defn -main [& _args]
  (let [server (io-server/stdio-server)]
    ;; server/start returns a promise that resolves when server shuts down
    ;; We need to deref it to keep the process alive
    @(server/start server {})))
