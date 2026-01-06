(ns com.zihao.replicant-main.replicant.ws-client
  (:require-macros
   [cljs.core.async.macros :as asyncm :refer [go go-loop]])
  (:require 
   [cljs.core.async :as async :refer [<!]]
   [taoensso.sente  :as sente]))

;; (defmethod ig/init-key :adapter/ws-client [_ _]
;;   (ws-client/make-ws-client))
(defn make-ws-client []
  (let [{:keys [chsk ch-recv send-fn state]}
        (sente/make-channel-socket-client!
         "/chsk"
         nil
         {:type :auto
          :host "localhost"
          :port 3000})]
    {:chsk chsk
     :ch-chsk ch-recv
     :chsk-send! send-fn
     :chsk-state state}))

(defn ws-handler [stop-ch {:keys [ch-chsk chsk-send!] :as ws-client}]
  (go-loop []
    (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
      (when (= port ch-chsk)
        (let [{:keys [event ?data id send-fn]} event-msg]
          (try
            (case id
              :chsk/ws-ping nil
              :subscribe/event (println 111)
              nil)
            (catch :default e
              (println "error" e))))
        (recur)))))

(defn make-ws-handler-with-extensions
  "Creates a WebSocket event handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [ws-client event-msg] and return non-nil if handled."
  [& extension-fns]
  (fn [stop-ch {:keys [ch-chsk chsk-send!] :as ws-client}]
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [event ?data id]} event-msg]
            (try
              ;; Try extension functions first
              (or (some #(when-let [result (% ws-client event-msg)]
                           result)
                       extension-fns)
                  ;; Fallback to built-in events
                  (case id
                    :chsk/ws-ping nil
                    nil))
              (catch :default e
                (js/console.error "WebSocket handler error:" e)))
            (recur)))))))

(comment
  (def stop-ch (async/chan))
  (ws-handler stop-ch nil)
  (async/put! stop-ch :stop)
  :rcf)

