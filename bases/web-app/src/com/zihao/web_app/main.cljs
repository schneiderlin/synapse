(ns com.zihao.web-app.main
  (:require
   [com.zihao.web-app.render :as render :refer [render-main]]
   [com.zihao.playground-drawflow.node :as node]
   [com.zihao.language-learn.fsrs.idxdb-actions :as fsrs-actions]
   [com.zihao.language-learn.lingq.actions :as lingq-actions]
   [com.zihao.web-app.router :as router]
   [com.zihao.replicant-main.replicant.main :as rm]
   [com.zihao.replicant-main.replicant.ws-client :as ws-client]
   [com.zihao.replicant-main.interface :as rm1]
   [com.zihao.replicant-component.interface :as replicant-component]
   [com.zihao.xiangqi.actions :as xiangqi-actions]
   [com.zihao.xiangqi.interface :as xiangqi]
   [integrant.core :as ig]
   [dataspex.core :as dataspex]
   [cljs.core.async :as async]))

(def config
  {:replicant/el "app"
   :replicant/store {}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action
                               xiangqi-actions/execute-action
                               fsrs-actions/execute-action
                               fsrs-actions/execute-effect
                               lingq-actions/execute-action]
   :ws/ws-client true
   :ws/ws-handler {:ws-client (ig/ref :ws/ws-client)
                   :ws-event-handlers (ig/ref :ws/event-handlers)
                   :store (ig/ref :replicant/store)}
   :ws/event-handlers [xiangqi/ws-event-handler-frontend]
   :replicant/render-loop {:system {:store (ig/ref :replicant/store)
                                    :el (ig/ref :replicant/el)
                                    :routes (ig/ref :replicant/routes)
                                    :interpolate (apply rm1/make-interpolate [])
                                    :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                                    :execute-actions (ig/ref :replicant/execute-actions)
                                    :base-url ""}
                           :hash-router? true}})

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

(defmethod ig/init-key :replicant/render-loop [_ {:keys [system hash-router?]}]
  (rm/init-render-loop render-main (assoc system :hash-router? hash-router?)))

(defmethod ig/init-key :ws/ws-client [_ enabled?]
  (when enabled?
    (ws-client/make-ws-client)))

(defmethod ig/init-key :ws/event-handlers [_ handlers]
  handlers)

(defmethod ig/init-key :ws/ws-handler [_ {:keys [store ws-client ws-event-handlers]}]
  (when ws-client
    (let [stop-ch (async/chan)
          handler (apply rm1/make-ws-handler-with-extensions ws-event-handlers)]
      (handler stop-ch (assoc ws-client :system {:replicant/store store}))
      {:stop-ch stop-ch})))

(defmethod ig/halt-key! :ws/ws-handler [_ {:keys [stop-ch]}]
  (async/put! stop-ch :stop))

(defonce !system (atom nil))

(defn ^:dev/after-load main []
  (let [system (ig/init config)
        store (:replicant/store system)]
    (dataspex/inspect "store" store)
    (reset! !system system)))

(comment
  @!system
  (def store (:replicant/store @!system))
  :rcf)
