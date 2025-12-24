(ns com.zihao.playground-drawflow.interface
  (:require
   [com.zihao.playground-drawflow.render :as render]
   #?(:cljs [com.zihao.playground-drawflow.actions :as actions])))

(defn render [state]
  (render/render state))

#?(:cljs
   (defn execute-action [{:keys [store]} event action args]
     (actions/execute-action store event action args)))

