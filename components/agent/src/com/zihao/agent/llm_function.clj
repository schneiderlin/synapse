(ns com.zihao.agent.llm-function
  (:require
   [clojure.walk :refer [keywordize-keys]]
   [libpython-clj2.python :as py]
   [com.zihao.cljpy-main.interface :as cljpy-main]
   [com.zihao.baml-client.interface :as baml-client :refer [reload]]
   [com.zihao.agent-eval.interface :as agent-eval]))

(comment
  (reload)

  (def python-env (cljpy-main/make-python-env ["./components/baml-client"]
                                              {:built-in "builtins"
                                               :sys "sys"
                                               :baml-client "baml_client"}))
  :rcf)

(defn pydantic->clj
  "Convert a Pydantic model to a Clojure map.
   Uses model_dump() for Pydantic v2 or dict() for v1.
   Recursively converts nested Pydantic models."
  [pydantic-model]
  (if (string? pydantic-model)
    ;; agent execute-action callback 需要返回 :message 和 :code, code 是 optional 的
    {:message {:role "assistant" :content pydantic-model}}
    (let [model-dump (py/py. pydantic-model "model_dump")]
      (keywordize-keys (py/->jvm model-dump)))))

(defn choose-tool [ctx message]
  (let [sync-client (py/import-module "baml_client.sync_client")
        b (py/py.- sync-client "b")]
    (-> (py/py. b "ChooseTool" (py/->py-dict message))
        pydantic->clj)))

(comment
  (def tool-result
    (choose-tool nil {:role "user" :content "I want to add 1 and 2"}))
  :rcf)

(defn decide-action
  "Call DecideAction which returns an ActionDecision with optional message and tools.
   messages: vector of Message objects
   tool: optional map with :name, :args, :result keys
   Returns a Clojure map with :message (may be nil) and :tools (array, may be empty)."
  [ctx messages tool]
  (let [sync-client (py/import-module "baml_client.sync_client")
        b (py/py.- sync-client "b")
        messages-list (py/->py-list (map py/->py-dict messages))
        args (if tool
               [messages-list (py/->py-dict tool)]
               [messages-list nil])
        result (py/py. b "DecideAction" (first args) (second args))]
    (pydantic->clj result)))

(comment
  ;; Test DecideAction - returns ActionDecision with optional message and tools
  (def res1 (decide-action nil [{:role "user" :content "Hello, how are you?"}] nil))
  ;; => {:message {:role "assistant", :content "..."}, :tools []}  

  (def res2 (decide-action nil [{:role "user" :content "I want to add 1 and 2"}] nil))
  ;; => {:message nil, :tools [{:name "add", :a 1, :b 2}]}

  (decide-action nil [{:role "user" :content "Add 1 and 2, then subtract 3 from 4"}] nil)
  ;; => {:message {:role "assistant", :content "..."}, :tools [{:name "add", :a 1, :b 2}]}

  ;; Test with tool result
  (decide-action nil
                 [{:role "user" :content "I want to add 1 and 2"}]
                 {:name "add" :args "{:a 1, :b 2}" :result "3"})
  ;; => {:message {:role "assistant", :content "The result is 3"}, :tools []}

  :rcf)

(defn- process-stream
  "Helper function to process a stream with optional callbacks.
   stream: Python stream object
   stream-callback: optional function to call with each partial result
   final-callback: optional function to call with the final result
   Returns the final result as a Clojure map."
  [stream stream-callback final-callback]
  (let [builtins (py/import-module "builtins")]
    ;; If stream-callback is provided, iterate over partial results
    (when stream-callback
      (let [iter-fn (py/py. builtins "iter" stream)]
        (loop [continue? true]
          (when continue?
            (let [has-next? (try
                              (let [partial-result (py/py. builtins "next" iter-fn)
                                    partial-clj (pydantic->clj partial-result)]
                                (stream-callback partial-clj)
                                true)
                              (catch Exception e
                                ;; StopIteration is raised when iterator is exhausted - this is expected
                                (let [msg (str (.getMessage e))
                                      class-name (str (class e))]
                                  (if (or (.contains msg "StopIteration")
                                          (.contains class-name "StopIteration"))
                                    false
                                    (throw e)))))]
              (recur has-next?))))))
    ;; Get and return the final response
    (let [final-result (py/py. stream "get_final_response")
          final-clj (pydantic->clj final-result)]
      ;; If final-callback is provided, call it with the final result
      (when final-callback
        (final-callback final-clj))
      final-clj)))

(defn code-act-agent
  "wrapper for baml function CodeActAgent"
  [{:keys [messages code code-result stream-callback final-callback]}]
  (let [sync-client (py/import-module "baml_client.sync_client")
        b (py/py.- sync-client "b")
        stream-client (py/py.- b "stream")
        messages-list (py/->py-list (map py/->py-dict messages))
        stream (py/py. stream-client "CodeActAgent" messages-list code code-result)]
    (process-stream stream stream-callback final-callback)))

(comment
  ;; Test CodeActAgent - returns CodeActAgentDecision with optional message and code
  (def res1 (code-act-agent
             {:messages [{:role "user" :content "what is in current dir"}]
              :code nil
              :code-result nil}))
  ;; => {:message nil, :code "(ls)"}

  (def res2 (code-act-agent {:messages [{:role "user" :content "how many files in current dir"}]
                             :code "(ls)"
                             :code-result "file1.clj\nfile2.clj\ndirectory1\ndirectory2"}))
  ;; => {:message {:role "assistant", :content "There are 2 files"}, :code nil}

  (code-act-agent {:messages [{:role "user" :content "how many files in current dir"}]
                   :code "(ls)"
                   :code-result "file1.clj\nfile2.clj\ndirectory1\ndirectory2"
                   :stream-callback (fn [partial-result]
                                      (println "partial-result:" partial-result))
                   :final-callback (fn [final-result]
                                     (println "final-result:" final-result))})

  :rcf)

(defn chat-agent
  "wrapper for baml function ChatAgent"
  [{:keys [messages stream-callback final-callback]}
   & {:keys [kwargs]}]
  (let [sync-client (py/import-module "baml_client.sync_client")
        b (py/py.- sync-client "b")
        stream-client (py/py.- b "stream")
        messages-list (py/->py-list (map py/->py-dict messages))
        stream (py/py* stream-client "ChatAgent" [messages-list] (py/->py-dict kwargs))]
    (process-stream stream stream-callback final-callback)))

(comment
  (require '[com.zihao.agent-eval.interface :as agent-eval])
  (def coll (agent-eval/create-collector {:cljpy/python-env python-env} "chat-agent"))

  ;; 没有 collector
  (def res (chat-agent {:messages [{:role "user" :content "hello"}]}))
  ;; 有 collector
  (def res (chat-agent {:messages [{:role "user" :content "hello"}]}
                       {:kwargs {:baml_options
                                 {"collector" coll}}}))

  (agent-eval/extract-log-data coll)
  (agent-eval/extract-http-request-response coll)

  (def last-log (py/py.- coll "last"))
  (py/py.- last-log "raw_llm_response")
  :rcf)

(defn code-locator
  "wrapper for baml function CodeLocator"
  [{:keys [messages stream-callback final-callback]}]
  (let [sync-client (py/import-module "baml_client.sync_client")
        b (py/py.- sync-client "b")
        stream-client (py/py.- b "stream")
        messages-list (py/->py-list (map py/->py-dict messages))
        stream (py/py. stream-client "CodeLocator" messages-list)]
    (process-stream stream stream-callback final-callback)))

(defn llm-caller
  ([ctx function-name args]
   (llm-caller ctx function-name args nil nil))
  ([ctx function-name args stream-callback]
   (llm-caller ctx function-name args stream-callback nil))
  ([ctx function-name args stream-callback final-callback]
   (case function-name
     :choose-tool (apply choose-tool ctx args)
     :decide-action (apply decide-action ctx args)
     nil)))

(comment
  (def res
    (llm-caller nil :decide-action [{:role "user" :content "I want to add 1 and 2"}]))

  :rcf)

