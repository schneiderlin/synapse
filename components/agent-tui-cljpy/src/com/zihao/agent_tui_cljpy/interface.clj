(ns com.zihao.agent-tui-cljpy.interface
  (:require [com.zihao.agent-tui-cljpy.tui :as tui]))

(defn reload []
  (tui/reload))

(defn get-terminal-app []
  (tui/get-terminal-app))

(defn tui [terminal-app opts]
  (tui/tui
   terminal-app
   opts))

(defn run [tui]
  (tui/run tui))

(defn set-handler [tui handler-fn]
  (tui/set-handler tui handler-fn))

(defn start-spinner [app-instance]
  (tui/start-spinner app-instance))

(defn add-user-output [app-instance message]
  (tui/add-user-output app-instance message))

(defn add-assistant-output [app-instance message]
  (tui/add-assistant-output app-instance message))

(defn add-clojure-code [app-instance code]
  (tui/add-clojure-code app-instance code))

(defn add-plain-text [app-instance text]
  (tui/add-plain-text app-instance text))

(defn stop-spinner [app-instance]
  (tui/stop-spinner app-instance))

(defn start-streaming-output [app-instance]
  (tui/start-streaming-output app-instance))

(defn append-streaming-output [app-instance text-chunk]
  (tui/append-streaming-output app-instance text-chunk))

(defn finish-streaming-output [app-instance]
  (tui/finish-streaming-output app-instance))

(defn set-streaming-output [app-instance text]
  (tui/set-streaming-output app-instance text))

(defn add-log [app-instance message]
  (tui/add-log app-instance message))
