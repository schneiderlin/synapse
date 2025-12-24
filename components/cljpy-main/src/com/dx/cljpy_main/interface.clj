(ns com.dx.cljpy-main.interface
  (:require 
   [com.dx.cljpy-main.python-interop :as python-interop]
   [com.dx.cljpy-main.bootstrap :as bootstrap]))

(defn make-python-env [user-lib-paths modules]
  (python-interop/make-python-env user-lib-paths modules))

(defn bootstrap []
  (bootstrap/bootstrap))
