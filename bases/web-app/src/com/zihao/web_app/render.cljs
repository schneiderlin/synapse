(ns com.zihao.web-app.render
  (:require-macros [com.zihao.replicant-main.replicant.utils :refer [build-admin?]])
  (:require
   [com.zihao.playground-drawflow.interface :as playground-drawflow]
   [com.zihao.replicant-main.replicant.navbar :refer [navbar]]))

(comment
  (build-admin?)
  :rcf)

(defn my-navbar []
  (navbar "Web App"
          [{:page-id :pages/frontpage :page-name "首页"}
           {:page-id :pages/playground-drawflow :page-name "Playground Drawflow"}
           ]))

(defn page-layout [content]
  [:div {:class ["min-h-screen" "bg-base-100"]}
   (my-navbar)
   [:div {:class ["container" "px-4" "py-6"]}
    content]])

(defn render-frontpage []
  (page-layout
   [:h1 "首页"]))

(defn render-playground-drawflow [state]
  (page-layout
   (playground-drawflow/render state)))

(defn render-not-found [_]
  (page-layout
   [:h1 "Not found!"]))

(defn render-main [state]
  (let [f (case (:location/page-id (:location state))
            :pages/frontpage render-frontpage 
            :pages/playground-drawflow render-playground-drawflow
            render-not-found)]
    (f state)))
