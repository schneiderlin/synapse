(ns com.zihao.agent.code-locator-agent
  (:require
   [com.brunobonacci.mulog :as u]
   [com.zihao.agent.llm-function :as llm-function]
   [com.zihao.agent.app :refer [add-log console-app]]
   [com.zihao.agent-tools.interface :as agent-tools]
   [libpython-clj2.python :as py]))

(defn- serialize-decision-to-json
  "Serialize LocatorDecision to JSON string"
  [decision]
  (let [json-module (py/import-module "json")]
    (py/py. json-module "dumps" (py/->py-dict decision))))

(defn- call-tool
  "Call the appropriate tool based on tool decision and return result as string"
  [tool-decision]
  (let [tool-name (:name tool-decision)]
    (cond
      (= tool-name "grep")
      (let [result (agent-tools/grep-tool-fn tool-decision)]
        (if (map? result)
          (:content result)
          (pr-str result)))

      (= tool-name "glob_file_search")
      (let [result (agent-tools/glob-tool-fn tool-decision)]
        (if (map? result)
          (:content result)
          (pr-str result)))

      (= tool-name "list_dir")
      (let [result (agent-tools/list-dir-tool-fn tool-decision)]
        (if (map? result)
          (:content result)
          (pr-str result)))

      :else
      (str "Unknown tool: " tool-name))))

(defn- final-callback [{:keys [store app]} final-result]
  (when app
    (add-log app (str "[INFO] Final callback received:" (pr-str final-result))))

  ;; Check if there's a tool to execute
  (if-let [tool-decision (:tool final-result)]
    ;; There's a tool to execute
    (do
      (when app
        (add-log app (str "[INFO] Tool decision found: " (pr-str tool-decision))))

      ;; Execute the tool
      (let [tool-result (call-tool tool-decision)
            tool-name (:name tool-decision)
            tool-args (pr-str tool-decision)]

        (when app
          (add-log app (str "[INFO] Tool executed: " tool-name ", result length: " (count tool-result))))

        ;; Append assistant message (the decision as JSON) to messages
        ;; Then append system message with tool result
        (let [messages (:messages @store)
              assistant-msg {:role "assistant"
                             :content (serialize-decision-to-json final-result)}
              tool-result-msg {:role "system"
                               :content (str "Tool executed: " tool-name "\n"
                                             "Arguments: " tool-args "\n"
                                             "Result: " tool-result)}
              updated-messages (-> messages
                                   (conj assistant-msg)
                                   (conj tool-result-msg))]

          ;; Update store with new messages (tool results are now in messages)
          (swap! store assoc :messages updated-messages)

          (when app
            (add-log app "[INFO] Store updated with tool result in messages, next iteration will call CodeLocator")))))

    ;; No tool - either findings or just explanation
    ;; Append assistant message to messages
    (let [assistant-msg {:role "assistant"
                         :content (serialize-decision-to-json final-result)}]
      (swap! store update-in [:messages] conj assistant-msg)
      (when app
        (add-log app "[INFO] Final result (no tool), appended to messages")))))

(defn get-actions [{:keys [store] :as ctx}]
  (let [state @store
        {:keys [messages]} state]
    [[:code-locator-agent
      {:messages messages
       :final-callback (partial final-callback ctx)}]]))

(defn execute-actions [_ctx actions]
  (doseq [action actions]
    (u/log ::execute-action :data action)
    (let [[action-type & args] action]
      (case action-type
        :code-locator-agent (llm-function/code-locator (first args))
        (throw (ex-info "Unknown action" {:action action-type}))))))

(defn agent-step [ctx]
  (let [actions (get-actions ctx)]
    (execute-actions ctx actions)))

(comment
  (require '[com.zihao.cljpy-main.interface :as cljpy-main])
  (def python-env (cljpy-main/make-python-env ["./components/baml-client"]
                                              {:built-in "builtins"
                                               :sys "sys"}))

  (def store (atom {:messages [{:role "user" :content "I want to find the code for the feature 'user authentication'"}]}))

  (agent-step {:store store :app console-app})
  :rcf)

