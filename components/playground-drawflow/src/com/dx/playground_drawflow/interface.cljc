(ns com.dx.playground-drawflow.interface
  (:require
   [com.dx.playground-drawflow.render :as render]
   #?(:cljs [com.dx.playground-drawflow.actions :as actions])))

(defn render [state]
  (render/render state))

#?(:cljs
   (defn execute-action [{:keys [store]} event action args]
     (actions/execute-action store event action args)))

