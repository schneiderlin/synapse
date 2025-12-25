(ns com.zihao.web-app.main
  (:require
   [com.zihao.web-app.render :as render :refer [render-main]]
   [com.zihao.web-app.router :as router]
   [com.zihao.replicant-main.replicant.main :as rm]
   [com.zihao.replicant-main.interface :as rm1]
   [com.zihao.replicant-main.replicant.ws-client :as ws-client]
   [com.zihao.replicant-component.interface :as replicant-component] 
   [clojure.core.async :as async]
   [integrant.core :as ig]
   [dataspex.core :as dataspex]))

(defn make-ws-handler [system]
  (fn [stop-ch {:keys [ch-chsk]}]
    (let [store (:replicant/store system)]
      (async/go-loop []
        (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
          (when (= port ch-chsk)
            (let [{:keys [?data id]} event-msg]
              (try
                (case id
                  :chsk/ws-ping nil
                  :agent/emit-text (let [{:keys [text]} ?data]
                                     (swap! store (fn [state]
                                                    (let [msgs (:msgs state [])
                                                          last-msg (last msgs)
                                                          streaming-text (:streaming-text state "")]
                                                      (if (and last-msg (= (:role last-msg) "assistant") (:streaming? last-msg))
                                                        ;; Append to existing streaming message
                                                        (-> state
                                                            (update :msgs (fn [ms]
                                                                            (conj (vec (butlast ms))
                                                                                  (assoc last-msg :content (str (:content last-msg) text)))))
                                                            (assoc :streaming-text (str streaming-text text)))
                                                        ;; Create new streaming message
                                                        (-> state
                                                            (update :msgs conj {:role "assistant"
                                                                                :content text
                                                                                :streaming? true})
                                                            (assoc :streaming-text text)))))))
                  :agent/partial-result (let [{:keys [text]} ?data]
                                          (swap! store (fn [state]
                                                         (let [msgs (:msgs state [])
                                                               last-msg (last msgs)]
                                                           (if (and last-msg (= (:role last-msg) "assistant") (:streaming? last-msg))
                                                             ;; Replace the whole message content
                                                             (-> state
                                                                 (update :msgs (fn [ms]
                                                                                 (conj (vec (butlast ms))
                                                                                       (assoc last-msg :content text))))
                                                                 (assoc :streaming-text text))
                                                             ;; Create new streaming message
                                                             (-> state
                                                                 (update :msgs conj {:role "assistant"
                                                                                     :content text
                                                                                     :streaming? true})
                                                                 (assoc :streaming-text text)))))))
                  :agent/response-end (swap! store (fn [state]
                                                     (let [msgs (:msgs state [])
                                                           last-msg (last msgs)]
                                                       (if (and last-msg (= (:role last-msg) "assistant") (:streaming? last-msg))
                                                         (-> state
                                                             (update :msgs (fn [ms]
                                                                             (conj (vec (butlast ms))
                                                                                   (dissoc last-msg :streaming?))))
                                                             (dissoc :streaming-text))
                                                         state))))
                  :agent/start-tool-call (let [{:keys [tool-name]} ?data]
                                           (swap! store assoc :tool-call-active? true
                                                  :current-tool-name tool-name))
                  :agent/end-tool-call (swap! store assoc :tool-call-active? false
                                              :current-tool-name nil)
                  nil)
                (catch :default e
                  (js/console.error "WebSocket handler error:" e)))
              (recur))))))))

(def config
  {:replicant/el "app"
   :replicant/store {:msgs [] :streaming-text "" :tool-call-active? false}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action]
   :ws/ws-client nil
   :ws/ws-handler {:ws-client (ig/ref :ws/ws-client)}
   :replicant/render-loop {:store (ig/ref :replicant/store)
                           :el (ig/ref :replicant/el)
                           :routes (ig/ref :replicant/routes)
                           :ws-client (ig/ref :ws/ws-client)
                           :interpolate (apply rm1/make-interpolate [])
                           :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                           :execute-actions (ig/ref :replicant/execute-actions)
                           :base-url "http://localhost:3000"}})

(defmethod ig/init-key :replicant/el [_ el]
  (rm/init-el _ el))

(defmethod ig/init-key :replicant/store [_ init-value]
  (atom init-value))

(defmethod ig/init-key :replicant/execute-actions [_ extension-fns]
  (apply rm1/make-execute-f extension-fns))

(defmethod ig/init-key :replicant/routes [_ routes]
  routes)

(defmethod ig/init-key :replicant/get-location-load-actions [_ get-location-load-actions]
  get-location-load-actions)

(defmethod ig/init-key :ws/ws-client [_ _]
  (ws-client/make-ws-client))

(defmethod ig/init-key :ws/ws-handler [_ _]
  (let [stop-ch (async/chan)]
    ;; Will be started after system is initialized
    stop-ch))

(defn start-ws-handler [system]
  (let [ws-client (:ws/ws-client system)
        stop-ch (:ws/ws-handler system)]
    (when (and ws-client stop-ch)
      (let [handler (make-ws-handler system)]
        (handler stop-ch ws-client)))))

(defmethod ig/halt-key! :ws/ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

(defmethod ig/init-key :replicant/render-loop [_ system]
  (rm/init-render-loop render-main system))

(defonce !system (atom nil))

(defn ^:dev/after-load main []
  (let [system (ig/init config)
        store (:replicant/store system)]
    (dataspex/inspect "store" store)
    (reset! !system system)
    ;; Start WS handler after system is initialized
    (start-ws-handler system)))

(comment
  @!system
  (def store (:replicant/store @!system))
  :rcf)
