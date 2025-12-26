(ns com.dx.agent-web.render
  (:require
   [component.ai.chat :as chat]))

(defn render-main [state]
  (let [msgs (:msgs state [])
        ;; Get content for input field
        content (:content state "")]
    [:div {:class ["min-h-screen" "bg-base-100" "p-4"]}
     [:div {:class ["max-w-4xl" "mx-auto" "h-screen" "flex" "flex-col"]}
      [:h1 {:class ["text-2xl" "font-bold" "mb-4"]}
       "Agent Chat"]
      [:div {:class ["flex-1" "overflow-auto"]}
       (chat/chat {:messages msgs
                   :content content})]]]))
