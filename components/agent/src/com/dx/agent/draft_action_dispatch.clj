(ns com.dx.agent.draft-action-dispatch
  (:require
   [com.dx.agent.interface :refer [IContext execute-actions ctx->actions agent-step]]
   [com.dx.baml-client.interface :as baml-client] 
   [com.dx.agent.llm-function :as llm-function]))

(comment
  (baml-client/reload)
  :rcf)

(defn get-actions [{:keys [store llm-caller] :as ctx}]
  (let [state @store
        {:keys [tool-call msgs]} state
        last-msg (last msgs)
        actions []
        has-pending-tool-call (and tool-call
                                   (not (contains? tool-call :result)))
        has-tool-result (and tool-call
                             (contains? tool-call :result))]
    (cond-> actions
      ;; Add action for pending tool calls
      has-pending-tool-call
      (conj [:use-tool tool-call])
      ;; Add action if there's a tool result - call LLM with tool info
      (and llm-caller
           has-tool-result)
      (conj [:llm-call :decide-action 
             [msgs
              {:name (name (:tool tool-call))
               :args (pr-str (:args tool-call))
               :result (pr-str (:result tool-call))}]
             (fn [ctx response]
               ;; Clear the tool-call since we've processed it
               ;; response is {:message {...} :tools [...]}
               (let [actions [[:dissoc-in [:tool-call]]]]
                 (cond-> actions
                   ;; If response contains message, append to msgs with role assistant
                   (:message response)
                   (conj [:conj [:msgs]
                          {:role "assistant"
                           :content (:content (:message response))}])
                   ;; If response contains tools, write first tool to ctx
                   (seq (:tools response))
                   (conj [:assoc-in [:tool-call]
                          (let [first-tool (first (:tools response))
                                tool-name (keyword (:name first-tool))
                                tool-args (dissoc first-tool :name)]
                            {:tool tool-name
                             :args tool-args})]))))])
      ;; Add action if last message is from user (but not if there's a pending tool call or tool result)
      (and llm-caller
           last-msg
           (= (:role last-msg) "user")
           (not has-pending-tool-call)
           (not has-tool-result))
      (conj [:llm-call :decide-action [msgs nil]
             (fn [ctx response]
               ;; response is {:message {...} :tools [...]}
               (let [actions []]
                 (cond-> actions
                   ;; If response contains message, append to msgs with role assistant
                   (:message response)
                   (conj [:conj [:msgs]
                          {:role "assistant"
                           :content (:content (:message response))}])
                   ;; If response contains tools, write first tool to ctx
                   (seq (:tools response))
                   (conj [:assoc-in [:tool-call]
                          (let [first-tool (first (:tools response))
                                tool-name (keyword (:name first-tool))
                                tool-args (dissoc first-tool :name)]
                            {:tool tool-name
                             :args tool-args})]))))]))))

(defrecord ToolCallContext [store tools llm-caller]
  IContext
  (ctx->actions [this]
    "Extract actions from context. Returns actions for pending tool calls or LLM calls.
     Multiple actions can be returned if multiple conditions are satisfied."
    (get-actions this))
  (execute-actions [this actions]
    "Execute actions as effect functions. Each action swaps! the store atom.
     Actions can be executed in parallel since they operate on the atom independently."
    (doseq [action actions]
      (let [[action-type & args] action]
        (case action-type
          ;; Primitive actions
          :assoc-in (let [[path value] args]
                      (swap! store assoc-in path value))
          :update-in (let [[path f & f-args] args]
                       (if (seq f-args)
                         (swap! store #(apply update-in % path f f-args))
                         (swap! store update-in path f)))
          :conj (let [[path value] args]
                  (swap! store update-in path conj value))
          :dissoc-in (let [[path key] args]
                       (swap! store update-in path dissoc key))

          ;; Domain-specific actions
          :use-tool (let [tool-call (first args)
                          {:keys [tool args]} tool-call
                          tool-spec (get tools tool)
                          tool-fn (get tool-spec :handler)
                          result (if tool-fn
                                   (tool-fn args)
                                   nil)]
                      (swap! store assoc-in [:tool-call]
                             (assoc tool-call :result result)))
          :llm-call (when llm-caller
                      (let [[function-name llm-args callback-fn] args
                            response (llm-caller this function-name llm-args)
                            follow-up-actions (when (and response callback-fn)
                                                (callback-fn this response))]
                        (when (seq follow-up-actions)
                          (execute-actions this follow-up-actions))))
          :user-input (swap! store update-in [:msgs] conj
                             {:type :message
                              :role "user"
                              :content (first args)})
          (throw (ex-info "Unknown action" {:action action-type})))))
    this)
  (persist-context [this]
    ;; No-op for simple in-memory context
    this))

(defn ctx->tools-description [{:keys [tools]}]
  (->> tools
       (map (fn [[tool-name tool-spec]]
              {:name tool-name
               :description (get tool-spec :description)}))
       (into [])))

(comment
  (ctx->tools-description
   (ToolCallContext. (atom {:msgs []})
                     {:add {:handler (fn add [{:keys [a b]}]
                                       (+ a b))
                            :description "Adds two numbers together \n Args: a: number, b: number"}}
                     nil))
  :rcf)

(def tools {:add {:handler (fn add [{:keys [a b]}]
                             (+ a b))
                  :description "Adds two numbers together"}
            :subtract {:handler (fn subtract [{:keys [a b]}]
                                  (- a b))
                       :description "Subtracts two numbers together"}})

;; tool call
(comment
  (def store (atom {:msgs []}))

  ;; LLM decide to use tool
  (-> (ToolCallContext. store
                        tools
                        llm-function/llm-caller)
      (execute-actions [[:user-input "what is the sum of 1 and 2?"]])
      #_(ctx->actions)
      (agent-step))

  ;; resolve pending tool call
  (-> (ToolCallContext. store
                        tools
                        llm-function/llm-caller)
      #_(ctx->actions)
      (agent-step))
  
  ;; call LLM again with tool result
  (-> (ToolCallContext. store
                        tools
                        llm-function/llm-caller)
      #_(ctx->actions)
      (agent-step)) 
  

  ;; bug? 
  :rcf)

;; skills
(comment
  "skill 其实就是 hierarchy 的 context.
   避免把所有东西一次性堆给 LLM, 这其实就是 ctx->state 做的东西.
   
   只不过这里 ctx->state 变成 LLM 决定一部分.
   告诉 LLM 有哪些技能, LLM 决定读详细的文档. 把这些文档后续也放到 prompt 里面就可以
   
   在什么时候触发 LLM 去判断是否读更多?
   每次有新的 novelty 进来的时候"
  :rcf)

