(ns com.zihao.replicant-main.replicant.actions
  (:require
   [clojure.edn :as reader]
   [com.zihao.replicant-main.replicant.query :as query]
   [com.zihao.replicant-main.replicant.command :as command]
   [com.zihao.replicant-main.replicant.router :as router]
   [com.zihao.replicant-main.replicant.hash-router :as hash-router]))

#?(:cljs
   (defn query-backend [{:keys [interpolate execute-actions store base-url] :as system} query & [{:keys [on-success]}]]
     (swap! store query/send-request (js/Date.) query)
     (-> (js/fetch (str base-url "/api/query")
                   #js {:method "POST"
                        :body (pr-str query)
                        :headers #js {"accept" "application/edn"
                                      "content-type" "application/edn"}})
         (.then #(.text %))
         (.then reader/read-string)
         (.then (fn [{:keys [result] :as res}]
                  (swap! store query/receive-response (js/Date.) query res)
                  (when on-success
                    (let [actions (if (fn? on-success)
                                    (on-success result)
                                    (let [result (interpolate result on-success)]
                                      #_(println "query result" result)
                                      result))]
                      (execute-actions system nil actions)))))
         (.catch #(swap! store query/receive-response
                         (js/Date.)
                         query {:error (.-message %)})))))

#?(:cljs
   (defn issue-command [{:keys [interpolate execute-actions store base-url] :as system} command & [{:keys [on-success]}]]
     (swap! store command/issue-command (js/Date.) command)
     (-> (js/fetch (str base-url "/api/command")
                   #js {:method "POST"
                        :body (pr-str command)
                        :headers #js {"accept" "application/edn"
                                      "content-type" "application/edn"}})
         (.then #(.text %))
         (.then reader/read-string)
         (.then (fn [result]
                  (swap! store command/receive-response
                         (js/Date.) command result)
                  (when on-success
                    (let [actions (if (fn? on-success)
                                    (on-success result)
                                    (let [result (interpolate result on-success)]
                                      #_(println "command result" result)
                                      result))]
                      (execute-actions system nil actions)))))
         (.catch #(swap! store command/receive-response
                         (js/Date.) command {:error (.-message %)})))))

#?(:cljs
 (defn choose-file [{:keys [interpolate execute-actions] :as system} _choose-args & [{:keys [on-success]}]]
  (let [input (js/document.createElement "input")]
    (set! (.-type input) "file")
    (set! (.-onchange input)
          (fn [e]
            (let [file (-> e .-target .-files (aget 0))]
              (when file
                (when on-success
                  (->> on-success
                       (interpolate e)
                       (execute-actions system nil)))))))
    (.click input))))

#?(:cljs
   (defn upload-file [{:keys [_interpolate execute-actions store base-url] :as system} command & [{:keys [on-success]}]]
     (let [file (:command/file command)
           form-data (js/FormData.)]
       (.append form-data "file" file)
       (.append form-data "payload" (pr-str (dissoc command :command/file)))
       (swap! store command/issue-command (js/Date.) command)
       (-> (js/fetch (base-url "/api/upload")
                     #js {:method "POST"
                          :body form-data})
           (.then #(.text %))
           (.then reader/read-string)
           (.then (fn [res]
                    (swap! store command/receive-response
                           (js/Date.) command res)
                    (when on-success
                      (execute-actions system nil on-success))))
           (.catch #(swap! store command/receive-response
                           (js/Date.) command {:error (.-message %)}))))))

(defn make-execute-f [& extension-fns]
  (letfn [(f [{:keys [store] :as system} e actions]
            (doseq [[action & args] actions]
              (let [result (or
                            (some #(when-let [result (% system e action args)]
                                     result)
                                  extension-fns)
                            (case action
                              :store/assoc (apply swap! store assoc args)
                              :store/assoc-in (apply swap! store assoc-in args)
                              :store/update-in (apply swap! store update-in args)
                              :event/prevent-default (.preventDefault e)
                              :event/stop-propagation  (.stopPropagation e)
                              :event/clear-input (set! (.-value (.-target e)) "")
                              :key/press (let [[k actions] args]
                                           (when (= (.-key e) k)
                                             (f system e actions)))
                              :router/navigate (let [navigate-fn (if (:hash-router? system) hash-router/navigate! router/navigate!)]
                                                 (navigate-fn system (first args)))
                              :data/query #?(:cljs (apply query-backend system args))
                              :data/command (apply issue-command system args)
                              :data/choose-file #?(:cljs (apply choose-file system args))
                              :data/upload #?(:cljs (apply upload-file system args))
                              :debug/print (js/console.log (clj->js args))
                              :clipboard/copy (js/navigator.clipboard.writeText (first args))
                              ;;  :timer/register (apply timer/register-timer args) 
                              (throw 
                               #?(:clj (ex-info (str "No matching action type found: " action ". Did you forget to register the action handler?") {:action action})
                                  :cljs (js/Error. (str "No matching action type found: " action ". Did you forget to register the action handler?"))))))]
                (when (vector? result)
                  (f system e result)))))]
    f))
