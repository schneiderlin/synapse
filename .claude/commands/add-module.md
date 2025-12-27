---
description: 添加子模块到 base 中
---

首先询问用户要把哪个子模块添加到哪个 base 中. 然后进行 前端注册 和 后端注册

## 前端注册

### extension functions
config 里面的 :replicant/render-loop 依赖到 :interpolate 和 :execute-actions.
interpolate 和 execute-actions 都有对应的 make-xxx 函数, 可以用 `(apply rm1/make-interpolate [])` 传一些 extensions function 进去.

每个模块会有自己的 interpolate 或者 execute-actions extention, 或者都有.
一般可以在 interface namespace 里面找到, 例如 com.zihao.language-learn.interface 里面有 execute-action.

需要把这个模块的 extention function 都在 main.cljs 里面注册.
```clojurescript
(require '[com.zihao.language-learn.interface :as ll])

;; inside config
:replicant/execute-actions [replicant-component/execute-action ll/execute-action]
```
interpolate 也是类似的方式注册.

### 路由
各模块一般有 router namespace, 在 base 的 router 里面注册.
```clojurescript
(ns com.zihao.web-app.router
  (:require
   [domkm.silk :as silk]
   [com.zihao.playground-drawflow.router :as playground-drawflow-router]
   [com.zihao.language-learn.router :as language-learn-router]))

(def routes
  (silk/routes
   (concat
    playground-drawflow-router/routes
    language-learn-router/routes
    [[:pages/frontpage [["home"]]]])))

(defn get-location-load-actions [location] 
  (or (playground-drawflow-router/get-location-load-actions location)
      (language-learn-router/get-location-load-actions location)
      ))
```

### render
在 base 的 render.cljs namespace, 注册各子模块的导航, 和 render-function, 例如
```clojurescript
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
   (list
    [:h1 "首页"]
    [:p "这是把几乎全部组件都用上, 带后端功能的页面. 如果想查看单独的前端组件, 跳转到"]
    [:a {:href "/portfolio.html"
         :target "_blank"
         :rel "noopener noreferrer"
         :class ["link" "link-primary" "font-medium"]} "前端组件页面"])))

(defn render-playground-drawflow [state]
  (page-layout
   (playground-drawflow/render state)))

(defn render-not-found [_]
  (render-frontpage))

(defn render-main [state]
  (let [f (case (:location/page-id (:location state))
            :pages/frontpage render-frontpage 
            :pages/playground-drawflow render-playground-drawflow
            render-not-found)]
    (f state)))
```

## 后端注册
修改 base 的 api namespace, 引用各子模块的 api namespace. 在 command handler 和 query handler 里面注册
```clojure
(ns com.zihao.web-app.api
  (:require
   [ring.adapter.jetty :as jetty]
   [com.zihao.jetty-main.interface :as jm] 
   [com.zihao.cljpy-main.interface :as cljpy-main]
   [com.zihao.language-learn.lingq.api :as lingq-api]
   [com.zihao.language-learn.fsrs.api :as fsrs-api]
   [integrant.core :as ig]))

(defn command-handler [system command] 
  (or (lingq-api/command-handler system command)
      (fsrs-api/command-handler system command)))

(defn query-handler [system query]
  (or (lingq-api/query-handler system query)
      (fsrs-api/query-handler system query)))
```

