(ns com.zihao.jetty-main.interface
  (:require [com.zihao.jetty-main.core :as core]))

(defn make-routes [system ws-server query-handler command-handler]
  (core/make-routes system ws-server query-handler command-handler))

(defn make-handler [routes & {:keys [public-dir]}]
  (core/make-handler routes :public-dir public-dir))
