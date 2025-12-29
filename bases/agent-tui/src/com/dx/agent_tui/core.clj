(ns com.dx.agent-tui.core 
  (:gen-class)
  (:require 
   [com.brunobonacci.mulog :as u]
   [clojure.core :exclude [println print]]
   [com.dx.cljpy-main.interface :as cljpy-main]
   [com.dx.agent-tui-cljpy.interface :as tui]
   [com.dx.agent-tui.agent :as agent]
   [com.dx.agent.resolve-at.resolve-at :as resolve-at]
   [com.dx.agent.code-executor :refer [code-executor]]))

(def python-env (cljpy-main/make-python-env ["./components/baml-client"
                                             "./components/agent-tui-cljpy"]
                                            {:built-in "builtins"
                                             :sys "sys"}))

(def terminal-app (tui/get-terminal-app))

(defonce store (atom {:msgs []}))

(defn -main [& args]
  #_(tel/stop-handlers!)
  (reset! store {:msgs []})
  (let [;; Create query candidates function that can be called from Python
        query-candidates-fn (fn [query]
                             (resolve-at/query-at-candidates query))
        app (tui/tui
             terminal-app
             {:query-candidates-fn query-candidates-fn})
        ctx {:store store
             :code-executor code-executor
             :app app}
        handler-fn (fn [app-self message]
                     (tui/start-spinner app-self)
                     ;; Add user input to context
                     (agent/execute-actions ctx [[:user-input message]])
                     ;; Loop through agent steps
                     (loop [step 0
                            max-steps 5]
                       (when (< step max-steps)
                         (tui/add-log app-self (str "step loop: " step))
                         (let [_ (agent/agent-step ctx)
                               has-more-actions (seq (agent/get-actions ctx))]
                           ;; Continue if there are more actions
                           (when has-more-actions
                             (tui/add-log app-self (str "has-more-actions: " has-more-actions))
                             (recur (inc step) max-steps)))))
                     (tui/stop-spinner app-self))]
    (tui/set-handler app handler-fn)
    (tui/run app)))

(comment
  (tui/reload)
  #_(tel/stop-handlers!)

  (-main)

  :rcf)



