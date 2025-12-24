(ns com.dx.agent.chat-agent
  (:require
   [taoensso.telemere :as tel]
   [com.dx.baml-client.interface :as baml-client]
   [com.dx.agent.llm-function :as llm-function]
   [com.dx.agent.app :refer [add-log add-assistant-output finish-streaming-output]]
   [com.dx.agent-eval.collector :as collector]))

(comment
  (baml-client/reload)
  :rcf)

(defn get-actions [{:keys [store app]}]
  (let [state @store
        {:keys [msgs]} state
        last-msg (last msgs)
        actions []]
    (when app
      (add-log app (str "[get-actions] Evaluating actions -"
                        ", last-msg-role: " (when last-msg (:role last-msg)))))
    (cond-> actions
      ;; Add action if last message is from user (but not if there's a pending code or code result)
      (and last-msg
           (= (:role last-msg) "user"))
      (conj [:chat-agent
             {:messages msgs
              :stream-callback (fn [partial-result]
                                 (when app
                                   (let [text (:content (:message partial-result))]
                                     (add-assistant-output app text))))
              :final-callback (fn [{:keys [message] :as final-result}]
                                (tel/log! {:level :info :msg "final-callback" :data final-result})
                                (when app
                                  (add-log app (str "[INFO] Final callback received:" (pr-str final-result)))
                                  (finish-streaming-output app))
                                ;; context
                                (when message
                                  (swap! store update-in [:msgs] conj {:role "assistant"
                                                                       :content (:content message)})))}]))))

(comment
  ;; 用户提问
  (get-actions {:store (atom {:msgs [{:role "user" :content "what is in current dir"}]})})

  ;; code 执行出结果
  (get-actions {:store (atom {:msgs [{:role "user" :content "what is in current dir"}
                                     {:role "assistant" :content "I will execute the code"}]
                              :code "(ls)"
                              :code-result "file1.clj\nfile2.clj\ndirectory1\ndirectory2"})})
  :rcf)

(defn execute-actions
  ([ctx actions] (execute-actions nil ctx actions))
  ([system {:keys [store app] :as ctx} actions]
   (doseq [action actions]
     (tel/log! {:level :info :msg "execute-action" :data action})
     (let [[action-type & args] action]
       (case action-type
         :chat-agent
         (if system
           ;; With system map: use Collector for logging
           (let [coll (collector/create-collector system "chat-agent")
                 result (llm-function/chat-agent args
                                                 {:kwargs {:baml_options
                                                           {"collector" coll}}})]
             (collector/save-collector-log! coll)
             result)
           ;; Without system map: normal call (backward compatible)
           (llm-function/chat-agent args))

         :user-input (swap! store update-in [:msgs] conj
                            {:type :message
                             :role "user"
                             :content (first args)})
         (throw (ex-info "Unknown action" {:action action-type})))))))

(defn agent-step
  ([ctx] (agent-step nil ctx))
  ([system ctx]
   (let [actions (get-actions ctx)]
     (execute-actions system ctx actions))))
