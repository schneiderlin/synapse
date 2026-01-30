(ns com.zihao.llm-eval.db
  (:require [datalevin.core :as d]))

(def db-uri "databases/llm-eval.db")

(def schema
  {:evaluation/id {:db/valueType :db.type/string
                   :db/cardinality :db.cardinality/one
                   :db/unique :db.unique/identity}
   :evaluation/input {:db/valueType :db.type/string
                      :db/cardinality :db.cardinality/one}
   :evaluation/output {:db/valueType :db.type/string
                       :db/cardinality :db.cardinality/one}
   :evaluation/model-name {:db/valueType :db.type/string
                           :db/cardinality :db.cardinality/one}
   :evaluation/timestamp {:db/valueType :db.type/instant
                          :db/cardinality :db.cardinality/one}
   :evaluation/prompt-metadata {:db/valueType :db.type/string
                                :db/cardinality :db.cardinality/one}
   :evaluation/scores {:db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/many
                       :db/isComponent true}

   :evaluation-score/id {:db/valueType :db.type/string
                         :db/cardinality :db.cardinality/one
                         :db/unique :db.unique/identity}
   :evaluation-score/criterion-name {:db/valueType :db.type/string
                                     :db/cardinality :db.cardinality/one}
   :evaluation-score/score-value {:db/valueType :db.type/float
                                  :db/cardinality :db.cardinality/one}
   :evaluation-score/judge-type {:db/valueType :db.type/string
                                 :db/cardinality :db.cardinality/one}
   :evaluation-score/feedback {:db/valueType :db.type/string
                               :db/cardinality :db.cardinality/one}
   :evaluation-score/evaluation {:db/valueType :db.type/ref
                                 :db/cardinality :db.cardinality/one}})

(def conn (d/get-conn db-uri schema))

(defn db-empty? []
  (let [query '[:find (count ?e) .
                :where [?e :evaluation/id]]
        eval-count (d/q query (d/db conn))]
    (zero? eval-count)))

(defn create-evaluation! [evaluation]
  (d/transact! conn [evaluation]))

(defn create-score! [score]
  (d/transact! conn [score]))

(defn get-evaluation-by-id [id]
  (let [query '[:find (pull ?e [* {:evaluation/scores [*]}]) .
                :in $ ?id
                :where [?e :evaluation/id ?id]]
        result (d/q query (d/db conn) id)]
    result))

(defn get-evaluations
  ([filters page size sort-field sort-order]
   (let [db (d/db conn)

         {:keys [model-names criterion-name score-min score-max
                 judge-type date-after date-before search-query]} filters

         base-query '[:find (pull ?e [* {:evaluation/scores [*]}]) .
                      :where [?e :evaluation/id]]

         all-evals (d/q base-query db)

         filtered-evals (filter (fn [eval]
                                  (and (or (nil? model-names)
                                           (some #(= % (:evaluation/model-name eval)) model-names))
                                       (or (nil? search-query)
                                           (or (.contains (:evaluation/input eval) search-query)
                                               (.contains (:evaluation/output eval) search-query)))
                                       (or (nil? criterion-name)
                                           (some #(and (= criterion-name (:evaluation-score/criterion-name %))
                                                       (or (nil? score-min)
                                                           (>= (:evaluation-score/score-value %) score-min))
                                                       (or (nil? score-max)
                                                           (<= (:evaluation-score/score-value %) score-max))
                                                       (or (nil? judge-type)
                                                           (= judge-type (:evaluation-score/judge-type %))))
                                                 (:evaluation/scores eval)))))
                                all-evals)

         total (count filtered-evals)

         total-pages (Math/ceil (/ total size))

         offset (* (dec page) size)

         sort-key (case sort-field
                    :timestamp :evaluation/timestamp
                    :model-name :evaluation/model-name
                    :evaluation/timestamp)

         sort-fn (if (= sort-order :asc)
                   #(compare (sort-key %1) (sort-key %2))
                   #(compare (sort-key %2) (sort-key %1)))

         sorted-evals (sort-by sort-key sort-fn filtered-evals)

         paged-evals (take size (drop offset sorted-evals))]
     {:data paged-evals
      :total total
      :page page
      :size size
      :total-pages (int total-pages)})))

(defn get-evaluation-stats []
  (let [db (d/db conn)

        total-query '[:find (count ?e) .
                      :where [?e :evaluation/id]]
        total-evaluations (d/q total-query db)

        model-query '[:find ?model (count ?e) .
                      :where [?e :evaluation/model-name ?model]]
        model-counts (into {} (d/q model-query db))

        avg-score-query '[:find ?criterion (float (avg ?score)) .
                          :where [?s :evaluation-score/criterion-name ?criterion]
                          [?s :evaluation-score/score-value ?score]]
        avg-scores (into {} (d/q avg-score-query db))

        date-query '[:find (min ?ts) (max ?ts) .
                     :where [?e :evaluation/timestamp ?ts]]
        [earliest latest] (if (zero? total-evaluations)
                            [(java.util.Date.) (java.util.Date.)]
                            (d/q date-query db))]
    {:total-evaluations total-evaluations
     :model-counts model-counts
     :avg-scores avg-scores
     :date-range {:earliest earliest
                  :latest latest}}))

(defn get-score-distributions []
  (let [db (d/db conn)

        query '[:find ?criterion ?judge ?score (count ?s) .
                :where [?s :evaluation-score/criterion-name ?criterion]
                [?s :evaluation-score/judge-type ?judge]
                [?s :evaluation-score/score-value ?score]]

        results (d/q query db)

        grouped (reduce (fn [acc [criterion judge score count]]
                          (let [key [criterion judge]]
                            (update-in acc [key :total] (fnil + 0) count)
                            (update-in acc [key :sum] (fnil + 0.0) (* score count))
                            (update-in acc [key :distribution] (fnil assoc {}) score count)))
                        {}
                        results)

        distributions (map (fn [[[criterion judge] {:keys [total sum distribution]}]]
                             {:criterion-name criterion
                              :judge-type judge
                              :distribution distribution
                              :total total
                              :avg (Math/round (* 100 (/ sum total)))})
                           grouped)]
    distributions))

(defn get-model-names []
  (let [db (d/db conn)
        query '[:find ?model .
                :where [?e :evaluation/model-name ?model]]]
    (sort (d/q query db))))

(defn get-criteria-names []
  (let [db (d/db conn)
        query '[:find ?criterion .
                :where [?s :evaluation-score/criterion-name ?criterion]]]
    (sort (d/q query db))))

(defn seed-data! []
  (when (db-empty?)
    (let [models ["gpt-4" "claude-3-opus" "claude-3-sonnet" "gemini-pro"]
          criteria ["accuracy" "helpfulness" "clarity" "safety"]
          sample-inputs ["What is the capital of France?"
                         "Explain quantum computing in simple terms."
                         "Write a haiku about programming."
                         "What are the main causes of climate change?"
                         "How do I bake a chocolate cake?"]
          sample-outputs ["The capital of France is Paris."
                          "Quantum computing uses quantum bits (qubits) that can exist in multiple states simultaneously, allowing for parallel processing of complex calculations."
                          "Code flows like water,\nBugs hide in the logic stream,\nDebug, compile, run."
                          "The main causes of climate change include greenhouse gas emissions from burning fossil fuels, deforestation, industrial processes, and agricultural activities."
                          "To bake a chocolate cake: preheat oven to 350°F, mix dry ingredients, combine wet ingredients, blend together, pour into greased pans, and bake for 30-35 minutes."]
          now (java.util.Date.)]

      (dotimes [i 50]
        (let [eval-id (format "eval-%04d" (inc i))
              model (nth models (mod i (count models)))
              timestamp (java.util.Date. (- (.getTime now) (* i 3600000)))
              input (nth sample-inputs (mod i (count sample-inputs)))
              output (nth sample-outputs (mod i (count sample-outputs)))
              prompt-metadata (str "{\"template\":\"standard_prompt\",\"variables\":{\"temperature\":0.7,\"max_tokens\":500},\"data_source\":\"sample_dataset_v1\"}")

              evaluation {:evaluation/id eval-id
                          :evaluation/input input
                          :evaluation/output output
                          :evaluation/model-name model
                          :evaluation/timestamp timestamp
                          :evaluation/prompt-metadata prompt-metadata
                          :evaluation/scores []}

              scores (for [criterion criteria
                           judge ["human" "llm"]]
                       {:evaluation-score/id (format "score-%s-%s-%s" eval-id criterion judge)
                        :evaluation-score/criterion-name criterion
                        :evaluation-score/score-value (float (if (= judge "human")
                                                               (+ 6 (rand-int 4))
                                                               (+ 5 (rand-int 4))))
                        :evaluation-score/judge-type judge
                        :evaluation-score/feedback (format "%s feedback for %s" judge criterion)})]

          (create-evaluation! evaluation

                              (doseq [score scores]
                                (create-score! (assoc score :evaluation-score/evaluation [:evaluation/id eval-id])))))))))

(comment
  (seed-data!)
  (get-evaluations {} 1 10 :timestamp :desc)
  (get-evaluation-stats)
  (get-score-distributions)
  (get-model-names)
  (get-criteria-names)
  :rcf)
