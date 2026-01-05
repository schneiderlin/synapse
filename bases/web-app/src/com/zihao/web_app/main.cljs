(ns com.zihao.web-app.main
  (:require
   [com.zihao.web-app.render :as render :refer [render-main]]
   [com.zihao.playground-drawflow.node :as node]
   [com.zihao.language-learn.fsrs.idxdb-actions :as fsrs-actions]
   [com.zihao.language-learn.lingq.actions :as lingq-actions]
   [com.zihao.web-app.router :as router]
   [com.zihao.replicant-main.replicant.main :as rm]
   [com.zihao.replicant-main.interface :as rm1]
   [com.zihao.replicant-component.interface :as replicant-component]
   [com.zihao.xiangqi.actions :as xiangqi-actions]
   [integrant.core :as ig]
   [dataspex.core :as dataspex]))

(def example-state1
  {:playground-drawflow/canvas
   {:nodes {"node-2" {:id "node-2"
                      :type "custom-card"
                      :position {:x 400 :y 100}
                      :data {:label "Card Node"
                             :content "Another custom body example"
                             :body-component node/custom-card-node-body}
                      :selected? false}
            "node-3" {:id "node-3"
                      :type "custom-card"
                      :position {:x 700 :y 100}
                      :data {:label "Card Node"
                             :content "Another custom body example"
                             :body-component node/custom-card-node-body}
                      :selected? false}}
    :edges []
    :viewport {:x 0 :y 0 :zoom 1}
    :handle-offsets {"node-2" {"input" {:target {:x 0 :y 65}}
                               "output" {:source {:x 150 :y 65}}}
                     "node-3" {"input" {:target {:x 0 :y 65}}
                               "output" {:source {:x 150 :y 65}}}}}})

(def config
  {:replicant/el "app"
   :replicant/store {:msgs [] :streaming-text "" :tool-call-active? false}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action
                               xiangqi-actions/execute-action
                               fsrs-actions/execute-action
                               fsrs-actions/execute-effect
                               lingq-actions/execute-action]
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
