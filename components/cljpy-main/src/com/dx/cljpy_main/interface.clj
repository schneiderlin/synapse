(ns com.zihao.cljpy-main.interface
  (:require 
   [com.zihao.cljpy-main.python-interop :as python-interop]
   [com.zihao.cljpy-main.bootstrap :as bootstrap]))

(defn make-python-env [user-lib-paths modules]
  (python-interop/make-python-env user-lib-paths modules))

(defn bootstrap []
  (bootstrap/bootstrap))
