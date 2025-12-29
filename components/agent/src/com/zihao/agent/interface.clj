(ns com.zihao.agent.interface
  (:require
   [com.zihao.agent.llm-function :as llm-function]
   [com.brunobonacci.mulog :as u]))

(defn code-act-agent [ctx {:keys [messages code code-result stream-callback final-callback] :as opts}]
  (llm-function/code-act-agent ctx opts))

;; ============================================================================
;; Protocols for Generalization
;; ============================================================================

(defprotocol IContext
  "Protocol for context management. In-memory store for agent state.
   The context contains a store (atom) that holds the current state."
  (ctx->actions [this]
    "Extract actions from context based on current state.
     Actions can be: :llm-call, :use-tool, or primitive actions like :assoc-in, :update-in, etc.
     Returns a sequence of actions.")
  (execute-actions [this actions]
    "Execute a sequence of actions. Actions are effect functions that swap! the store.
     Actions can be executed in parallel since they operate on the atom independently.
     Returns the context (for chaining).")
  (persist-context [this]
    "Persist in-memory state to storage. Returns context (may be updated)."))

;; ============================================================================
;; Core Agent Loop
;; ============================================================================

(defn agent-step
  "Single step of the agent loop. Discovers actions from context and executes them.
   Users can add novelty via execute-actions (e.g., [:user-input \"...\"]) at the top level.
   Actions are effect functions that swap! the store, enabling parallel execution.

   Args:
   - context: An IContext implementation (contains a store atom)
   
   Returns:
   - The context (for chaining)"
  [context]
  (let [actions (ctx->actions context)]
    (u/log ::actions :data actions)
    (execute-actions context actions)))

(defn agent-loop
  "Run the agent loop until no more actions are available or max-steps is reached.
   Calls agent-step repeatedly until the agent has completed all its work.

   Args:
   - context: An IContext implementation (contains a store atom)
   - max-steps: Maximum number of steps to execute (default: 20)
   
   Returns:
   - The context (for chaining)"
  ([context] (agent-loop context 20))
  ([context max-steps]
   (loop [step 0
          ctx context]
     (if (>= step max-steps)
       ctx
       (let [actions (ctx->actions ctx)]
         (if (empty? actions)
           ctx
           (recur (inc step) (agent-step ctx))))))))
