(ns com.dx.agent-web.actions
  (:require
   [clojure.string :as str]))

(defn execute-action
  "Handle agent-related actions"
  [{:keys [store ws-client] :as system} _event action args]
  (case action
    :ai/send-user-message
    (let [[path content] args
          {:keys [text content]} content 
          {:keys [chsk-send!]} ws-client
          _ (println "execute-action" content)]
      (when content
        ;; Add user message to store 
        (swap! store (fn [state]
                       (-> state
                           (update :msgs conj {:role "user"
                                               :content content})
                           (assoc :content "")))) ; Clear input
        ;; Send message to backend via WebSocket
        (when chsk-send!
            (chsk-send! [:ai/send-user-message {:content content}] 5000
                        (fn [reply]
                          (js/console.log "Message sent, reply:" reply))))))
    nil))

