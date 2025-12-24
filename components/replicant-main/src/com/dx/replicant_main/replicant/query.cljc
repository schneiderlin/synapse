(ns com.dx.replicant-main.replicant.query)

(defn take-until [f xs]
  (loop [res []
         xs (seq xs)]
    (cond
      (nil? xs) res
      (f (first xs)) (conj res (first xs))
      :else (recur (conj res (first xs)) (next xs)))))

(defn add-log-entry [log entry]
  (cond->> (cons entry log)
    (= :query.status/success (:query/status entry))
    (take-until #(= (:query/status %) :query.status/loading))))

(defn send-request [state now query]
  (update-in state [::log query] add-log-entry
             {:query/status :query.status/loading
              :query/user-time now}))

(defn ^{:indent 2} receive-response [state now query response] 
  (update-in state [::log query] add-log-entry
             (cond-> {:query/status (if (:success? response)
                                      :query.status/success
                                      :query.status/error)
                      :query/user-time now}
               (:success? response)
               (assoc :query/result (:result response)))))

(comment
  (let [state {}
        query {:query/kind :query/session}]
    (-> state
        (send-request 1 query)
        (receive-response 1
                          query
                          {:success? true :result 42})
        (get-result query)))

  :rcf)

(defn get-log [state query]
  (get-in state [::log query]))

(defn get-latest-status [state query]
  (:query/status (first (get-log state query))))

(defn loading? [state query]
  (= :query.status/loading
     (get-latest-status state query)))

(defn available? [state query]
  (->> (get-log state query)
       (some (comp #{:query.status/success} :query/status))
       boolean))

(defn error? [state query]
  (= :query.status/error
     (get-latest-status state query)))

(defn get-result [state query]
  (->> (get-log state query)
       (keep :query/result)
       first))

(defn requested-at [state query]
  (->> (get-log state query)
       (drop-while #(not= :query.status/loading (:query/status %)))
       first
       :query/user-time))

#?(:cljs
   (defn parse-int [s default]
     (try
       (js/parseInt s 10)
       (catch :default _ default)))
   :clj
   (defn parse-int [s default]
     (try
       (java.lang.Integer/parseInt s)
       (catch Exception _ default))))

(defn get-page [location]
  (let [query-params (or (:location/query-params location) {})
        page (max 1 (parse-int (get query-params :page "1") 1)) ]
    page))

(defn get-size [location]
  (let [query-params (or (:location/query-params location) {})
        size (max 1 (parse-int (get query-params :size "20") 1)) ]
    size))