(ns com.dx.replicant-main.replicant.timer)

(defn trigger? [last-tick interval]
  (let [now (js/Date.)]
    (and (pos? interval)
         (>= (- now last-tick) interval))))

(comment
  
  (trigger? (- (js/Date.) 1001) 1000)

  (let [now (js/Date.)]
    (- now 1754376112807))
  :rcf)

(defonce !timers (atom {}))

;; Global timer reference to track the current interval
(defonce !current-timer (atom nil))

(defn clear-current-timer []
  (when @!current-timer
    (js/clearInterval @!current-timer)
    (reset! !current-timer nil)))

#_(defn start-timer [store]
  (clear-current-timer)
  (reset! !current-timer
          (js/setInterval
           (fn []
             (doseq [[timer-key {:keys [last-tick interval task]}] @!timers]
               (when (trigger? last-tick interval)
                 (task {:replicant/store store
                        :replicant/dispatch (partial dispatch-actions store)})
                 (swap! !timers assoc-in [timer-key :last-tick] (js/Date.)))))
           1000)))

(defn register-timer [timer-key interval-ms task-f]
  (swap! !timers assoc timer-key {:last-tick (js/Date.)
                                  :interval interval-ms
                                  :task task-f}))

(defn unregister-timer [timer-key]
  (swap! !timers dissoc timer-key))
