(ns com.zihao.web-app.render
  (:require-macros [com.zihao.replicant-main.replicant.utils :refer [build-admin?]])
  (:require
   [com.zihao.playground-drawflow.interface :as playground-drawflow]
   [com.zihao.xiangqi.render :as xiangqi-render]
   [com.zihao.login.login :as login]
   [com.zihao.language-learn.fsrs.render :as fsrs-render]
   [com.zihao.language-learn.lingq.render :as lingq-render]
   [com.zihao.llm-eval.interface :as llm-eval]
   [com.zihao.replicant-main.replicant.navbar :refer [navbar]]))

(comment
  (build-admin?)
  :rcf)

(defn my-navbar []
  (navbar "Web App"
          [{:page-id :pages/frontpage :page-name "首页"}
           {:page-id :pages/language-learn :page-name "FSRS 重复间隔"}
           {:page-id :pages/lingq :page-name "Lingq 阅读器"}
           {:page-id :pages/llm-eval-dashboard :page-name "LLM Eval Dashboard"}
           {:page-id :pages/llm-eval-dataset :page-name "LLM Eval Dataset"}
           {:page-id :pages/playground-drawflow :page-name "Playground Drawflow"}
           {:page-id :pages/xiangqi :page-name "象棋"}]))

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

(defn render-xiangqi [state]
  (xiangqi-render/main state))

(defn render-login [state]
  (login/login-form state))

(defn render-change-password [state]
  (login/change-password-form state))

(defn render-language-learn [state]
  (page-layout
   (fsrs-render/flashcard-page state)))

(defn render-lingq [state]
  (page-layout
   (lingq-render/main state)))

#_(defn render-llm-eval-dashboard [state]
  (page-layout
   (llm-eval/render-dashboard state)))

#_(defn render-llm-eval-dataset [state]
  (page-layout
   (llm-eval/render-dataset state)))

#_(defn render-llm-eval-detail [state]
  (page-layout
   (llm-eval/render-detail state)))

(defn render-llm-eval-workflow [state]
  (page-layout
   [:div {:class ["alert" "alert-info"]}
    [:span "Workflow editor coming soon!"]]))

(defn render-not-found [_]
  (render-frontpage))

(defn render-main [state]
  (let [f (case (:location/page-id (:location state))
            :pages/frontpage render-frontpage
            :pages/playground-drawflow render-playground-drawflow
            :pages/xiangqi render-xiangqi
            :pages/login render-login
            :pages/change-password render-change-password
            :pages/language-learn render-language-learn
            :pages/lingq render-lingq
            ;; :pages/llm-eval-dashboard render-llm-eval-dashboard
            ;; :pages/llm-eval-dataset render-llm-eval-dataset
            ;; :pages/llm-eval-detail render-llm-eval-detail
            :pages/llm-eval-workflow render-llm-eval-workflow
            render-not-found)]
    (f state)))
