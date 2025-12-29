(ns com.zihao.playground-jsonrpc.client
  (:require
   [com.brunobonacci.mulog :as u]
   [clojure.java.io :as io]
   [clojure.core.async :as async]
   [jsonrpc4clj.io-server :as io-server]
   [jsonrpc4clj.server :as server]
   [babashka.process :as p]))

(def server
  (p/process ["clojure" "-M" "-m" "com.zihao.playground-jsonrpc.simple-server"]
             {:err (io/file "components/playground-jsonrpc" "stderr.txt")
              :dir "components/playground-jsonrpc/"}))

(def client (io-server/server {:in (:out server)
                               :out (:in server)}))

(def start-promise (server/start client {}))

(comment 
  (:in server)
  (:out server) 

  (async/go-loop []
    (when-let [[level & args] (async/<! (:log-ch client))]
      (u/log ::log :data {:level level :args args})
      (recur))) 
  
  (let [request (server/send-request client "greet"
                                     {:val 42})
        response (server/deref-or-cancel request 5e3 ::timeout)]
    (if (= ::timeout response)
      (u/log ::error :exception (ex-info "No response from server after 5 seconds." {}))
      (do
        (u/log ::log :data {:response response
                   :data {:response response
                          :msg "Received response from server"}})
        response)))
  
  (server/shutdown client)
  :rcf)


