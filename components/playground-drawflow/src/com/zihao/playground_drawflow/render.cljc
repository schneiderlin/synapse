(ns com.zihao.playground-drawflow.render
  (:require
   #?(:cljs [com.zihao.playground-drawflow.core :as core]
      :clj [clojure.core :as core])))

(defn render
  [state]
  #?(:cljs (core/render-canvas state)
     :clj [:div "Canvas (CLJ placeholder)" state]))

