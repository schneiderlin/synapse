(ns com.zihao.jetty-main.interface
  (:require [com.zihao.jetty-main.core :as core]))

(defn make-routes [system ws-server query-handler command-handler]
  (core/make-routes system ws-server query-handler command-handler))

(defn make-handler [routes & {:keys [public-dir]}]
  (core/make-handler routes :public-dir public-dir))

(defn make-ws-handler-with-extensions
  "Creates a WebSocket handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [system event-msg] and return non-nil if handled."
  [& extension-fns]
  (apply core/make-ws-handler-with-extensions extension-fns))
