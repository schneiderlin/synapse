(ns com.zihao.web-app.progressive-enhancement
  (:require
   [clojure.edn :as edn]
   [replicant.dom :as r] 
   [com.zihao.web-app.router :as router] 
   [com.zihao.replicant-main.interface :as rm1]
   [com.zihao.replicant-component.interface :as replicant-component]
   [com.zihao.xiangqi.actions :as xiangqi-actions]
   [integrant.core :as ig]
   [dataspex.core :as dataspex]))

(def config
  {:replicant/store {}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action xiangqi-actions/execute-action]
   :replicant/render-loop {:store (ig/ref :replicant/store)
                           :routes (ig/ref :replicant/routes)
                           :interpolate (apply rm1/make-interpolate [])
                           :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                           :execute-actions (ig/ref :replicant/execute-actions)
                           :base-url "http://localhost:3000"}})

(defmethod ig/init-key :replicant/store [_ init-value]
  (atom init-value))

(defmethod ig/init-key :replicant/execute-actions [_ extension-fns]
  (apply rm1/make-execute-f extension-fns))

(defmethod ig/init-key :replicant/routes [_ routes]
  routes)

(defmethod ig/init-key :replicant/get-location-load-actions [_ get-location-load-actions]
  get-location-load-actions)

(defn render-counter [_state]
  "this is a counter")

(defn get-render-fn [type]
  ;; get render function based on type
  (case type
    "counter" render-counter
    :counter render-counter
    (throw (ex-info "Unknown replicant type" {:type type}))))

(defn get-initial-state [el]
  ;; get initial state from data-replicant/initial-state attribute
  (let [initial-state-str (.getAttribute el "x-data-replicant-initial-state")]
    (println "initial-state-str" initial-state-str)
    (if initial-state-str
      (edn/read-string initial-state-str)
      {})))

(defmethod ig/init-key :replicant/render-loop [_ {:keys [store]}]
  ;; Find all elements with data-replicant/type attribute
  (let [elements (array-seq (.querySelectorAll js/document "[x-data-replicant-type]"))]
    (doseq [[idx el] (map-indexed vector elements)]
      (let [type-str (.getAttribute el "x-data-replicant-type")
            type (keyword type-str)
            initial-state (get-initial-state el)
            element-store (atom initial-state)
            render-fn (get-render-fn type)]
        ;; Set up watch for this element
        (add-watch
         element-store ::render
         (fn [_ _ _ state]
           (r/render el (render-fn state))))
        ;; Store reference to element-store in the main store (use index if no id)
        (swap! store assoc-in [:elements (or (.-id el) (str "element-" idx)) :store] element-store)
        ;; Initial render
        (r/render el (render-fn initial-state)))))
  (swap! store assoc
         :app/started-at (js/Date.)))

(defonce !system (atom nil))

(defn ^:dev/after-load main []
  (let [system (ig/init config)
        store (:replicant/store system)]
    (println "start progressive enhancement")
    (dataspex/inspect "store" store)
    (reset! !system system)))

(comment
  @!system
  (def store (:replicant/store @!system))
  :rcf)
