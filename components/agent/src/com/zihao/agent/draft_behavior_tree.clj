(ns com.zihao.agent.draft-behavior-tree
  (:require [libpython-clj2.python :as py] 
            [ai.obney.grain.behavior-tree-v2.interface :as bt]
            [com.zihao.agent.tui :as tui]))

(defn st-memory-has-value? [{{:keys [path _schema]} :opts
                             :keys [st-memory]}]
  (let [st-memory-state @st-memory]
    (if path
      (get-in st-memory-state path)
      st-memory-state)))

(comment
  (st-memory-has-value? {:opts {:path [:question]} :st-memory (atom {})})
  (st-memory-has-value? {:opts {:path [:question]} :st-memory (atom {:question "test"})})
  :rcf)

(defn dspy [{{:keys [id signature operation]} :opts
             :keys [st-memory]
             :as context}]
  ;; Fake dspy - always succeeds and updates st-memory with a fake answer
  (let [state @st-memory
        question (get state :question)]
    (if question
      (do
        ;; Create a fake answer based on the question
        (swap! st-memory assoc :answer (str "Fake response to: " question))
        (println (str "✓ " id) "completed successfully (fake)")
        bt/success)
      (do
        (println (str "✗ " id " - missing question in st-memory"))
        bt/success))))

(comment
  (def st-memory (atom {:question "test"}))
  (dspy {:opts {:id :chat 
                :signature :chat 
                :operation :chain-of-thought} 
         :st-memory st-memory})
  :rcf)

(def behavior-tree
  [:sequence
   ;; Check short-term memory for the user's question
   [:condition
    {:path [:question]
     :schema :string}
    st-memory-has-value?]

   ;; Chat interaction with an LLM
   [:action
    {:id :chat
     :signature :chat
     :operation :chain-of-thought}
    dspy]

   ;; Check short-term memory for the answer
   [:condition
    {:path [:answer]
     :schema :string}
    st-memory-has-value?]])

(comment

  (do 
    (def bt (bt/build behavior-tree {}))
    
    ;; 启动 tui
    (tui/tui
     {:handler-fn (partial tui/handle-input {:bt bt})}))
  
  ;; st-memory in bt
  @(get-in bt [:context :st-memory])
  
  :rcf)
