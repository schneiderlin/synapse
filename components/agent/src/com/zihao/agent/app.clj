(ns com.zihao.agent.app
  (:require
   [com.zihao.agent-tui-cljpy.interface :as tui]))

(defn add-log [app s]
  ((:add-log app) s))

(defn add-assistant-output [app s]
  ((:add-assistant-output app) s))

(defn add-clojure-code [app s]
  ((:add-clojure-code app) s))

(defn add-plain-text [app s]
  ((:add-plain-text app) s))

(defn finish-streaming-output [app]
  (when-let [finish-fn (:finish-streaming-output app)]
    (finish-fn)))

(def tui-app
  {:add-log tui/add-log
   :add-assistant-output tui/add-assistant-output
   :add-clojure-code tui/add-clojure-code
   :add-plain-text tui/add-plain-text
   :finish-streaming-output tui/finish-streaming-output})

(def console-app
  {:add-log (fn [s] (println s))
   :add-assistant-output (fn [s] (println s))
   :add-clojure-code (fn [s] (println s))
   :add-plain-text (fn [s] (println s))
   :finish-streaming-output (fn [] nil)})
