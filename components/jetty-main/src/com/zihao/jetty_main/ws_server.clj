(ns com.zihao.jetty-main.ws-server 
  (:require
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.community.jetty :refer [get-sch-adapter]]))

(defn make-ws-server []
  (let [{:keys [ch-recv send-fn connected-uids
                ajax-post-fn ajax-get-or-ws-handshake-fn]}
        (sente/make-channel-socket-server! (get-sch-adapter) {:user-id-fn (constantly "uid")
                                                              :csrf-token-fn nil
                                                              :ws-kalive-ms (* 1000 10)})]

    {:ring-ajax-post ajax-post-fn
     :ring-ajax-get-or-ws-handshake ajax-get-or-ws-handshake-fn #_(fn [req]
                                                                    (let [result (ajax-get-or-ws-handshake-fn req)]
                                                                      #_(println "result" result)
                                                                      result))
     :ch-chsk ch-recv
     :chsk-send! send-fn
     :connected-uids connected-uids}))
