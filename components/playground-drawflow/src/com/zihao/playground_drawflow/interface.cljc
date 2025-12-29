(ns com.zihao.playground-drawflow.interface
  (:require
   [com.zihao.playground-drawflow.render :as render]
   #?(:cljs [com.zihao.playground-drawflow.actions :as actions])))

(defn render [state]
  (render/render state))

#?(:cljs
   (defn execute-action [system event action args]
     (actions/execute-action system event action args)))

