(ns com.dx.agent-tui-cljpy.tui
  (:require
   [libpython-clj2.python :as py :refer [py.]]
   [ai.obney.grain.behavior-tree-v2.interface :as bt]
   [ai.obney.grain.behavior-tree-v2.interface.protocol :as btp]
   [com.dx.cljpy-main.interface :as cljpy-main]))

(comment
  (def python-env (cljpy-main/make-python-env ["./components/agent-tui-cljpy"]
                                              {:built-in "builtins"
                                               :sys "sys"}))
  :rcf)

(defn reload 
  "Reload the Python app module to pick up changes to the TUI."
  []
  (let [importlib (py/import-module "importlib")
        sys (py/import-module "sys")
        modules (py/py.- sys "modules")
        ;; Reload the app module and related modules in reverse dependency order
        modules-to-reload (reverse ["app"])]
    (doseq [module-name modules-to-reload]
      (when (get modules module-name)
        (py/py. importlib "reload" (get modules module-name))))))

(defn get-terminal-app []
  (let [app-module (py/import-module "app")]
    (py/py.- app-module "TerminalApp")))

(defn start-spinner [app-instance]
  (py. app-instance "start_spinner"))

(defn add-user-output [app-instance message]
  (py. app-instance "add_user_output" message))

(defn add-assistant-output [app-instance message]
  (py. app-instance "add_assistant_output" message))

(defn add-clojure-code [app-instance code]
  (py. app-instance "add_clojure_code" code))

(defn add-plain-text [app-instance text]
  (py. app-instance "add_plain_text" text))

(defn stop-spinner [app-instance]
  (py. app-instance "stop_spinner"))

(defn start-streaming-output [app-instance]
  (py. app-instance "start_streaming_output"))

(defn append-streaming-output [app-instance text-chunk]
  (py. app-instance "append_streaming_output" text-chunk))

(defn finish-streaming-output [app-instance]
  (py. app-instance "finish_streaming_output"))

(defn set-streaming-output [app-instance text]
  (py. app-instance "set_streaming_output" text))

(defn add-log [app-instance message]
  (py. app-instance "add_log" message))

(defn handle-input
  [{:keys [bt st-memory-init]} this message]
  (future
    ;; Interact with Textual
    (start-spinner this)
    (add-user-output this message)

    ;; Execute the behavior tree
    (let [st-memory (get-in bt [:context :st-memory])
          _ (reset! st-memory (or st-memory-init {}))
          _ (swap! st-memory assoc :question message)
          _result (loop [i (range 10)
                         result (bt/run bt)]
                    (when (get-in bt [:context :lt-memory])
                      (add-assistant-output this (str "> " (-> bt :context :lt-memory btp/latest :reasoning_chain last))))
                    (when (and (seq i) (not= bt/success result))
                      (recur (rest i) (bt/run bt))))
          answer (get @st-memory :answer)]

      ;; Interact with Textual 
      (add-assistant-output this answer)
      (stop-spinner this)))
  nil)

(defn tui
  "return: the app instance"
  [terminal-app
   {:keys [handler-fn conversation-history query-candidates-fn] :or {conversation-history []}}]
  (let [terminal-app (terminal-app
                      :clojure_handler handler-fn
                      :conversation_history conversation-history
                      :query_candidates_fn query-candidates-fn)]
    terminal-app))

(defn set-handler [tui handler-fn]
  (py. tui "set_handler" handler-fn))

(defn run [tui]
  (py. tui "run"))
