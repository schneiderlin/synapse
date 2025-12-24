(ns com.zihao.playground-drawflow.render
  (:require
   [clojure.string :as str]
   #?(:cljs [com.zihao.playground-drawflow.core :as core])))

(defn render
  [state]
  #?(:cljs (core/render-canvas state)
     :clj [:div "Canvas (CLJ placeholder)"]))

