(ns com.dx.jetty-main.ws-server 
  (:require
   [taoensso.sente :as sente]
   [taoensso.sente.server-adapters.community.jetty :refer [get-sch-adapter]]
   [clojure.core.async :as async :refer [go-loop go <! <!!]]
   [taoensso.telemere :as tel]))

;; (defmethod ig/init-key :adapter/ws-server [_ _]
;;   (ws-server/make-ws-server))
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

(defn make-ws-command-handler [{:keys [command-handler] :as system}]
  (fn [{:keys [event uid client-id ?data id ?reply-fn]}]
    (let [command ?data
          result (command-handler system command)]
      (when ?reply-fn
        (?reply-fn result)))))

(defn make-ws-query-handler [{:keys [query-handler] :as system}]
  (fn [{:keys [event uid client-id ?data id ?reply-fn]}]
    (let [query ?data
          result (query-handler system query)]
      (println "result" query result)
      (when ?reply-fn
        (?reply-fn result)))))

(defn make-ws-handler [system]
  (fn [stop-ch {:keys [ch-chsk] :as ws-server-arg}]
    (let [ws-command-handler (make-ws-command-handler system)
          ws-query-handler (make-ws-query-handler system)]
      (go-loop []
        (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
          (when (= port ch-chsk)
            (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg]
              (try
                (case id
                  :chsk/ws-ping nil
                  :test/echo (?reply-fn [id ?data])
                  :data/query (ws-query-handler event-msg)
                  :data/command (ws-command-handler event-msg)
                  nil)
                (catch Exception e
                  (tel/error! e))))
            (recur)))))))