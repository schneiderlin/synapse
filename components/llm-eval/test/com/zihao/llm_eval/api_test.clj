(ns com.zihao.llm-eval.api-test
  (:require [clojure.test :refer :all]
            [com.zihao.llm-eval.api :as api]))

(deftest query-evaluations-test
  (testing "Query evaluations command handler"
    (let [cmd {:command/kind :query/evaluations
               :query-id 1
               :command/data {:filters {}
                              :page 1
                              :size 10
                              :sort {:field :timestamp :order :desc}}}
          result (api/query-handler cmd)]
      (is (map? result))
      (is (= :data/query-result (:command/kind result)))
      (is (= 1 (:query-id result)))
      (is (contains? result :result)))))

(deftest query-evaluation-by-id-test
  (testing "Query evaluation by id command handler"
    (let [cmd {:command/kind :query/evaluation-by-id
               :query-id 2
               :command/data {:id "eval-0001"}}
          result (api/query-handler cmd)]
      (is (map? result))
      (is (= :data/query-result (:command/kind result))))))

(deftest query-evaluation-stats-test
  (testing "Query evaluation stats command handler"
    (let [cmd {:command/kind :query/evaluation-stats
               :query-id 3
               :command/data {:filters {}}}
          result (api/query-handler cmd)]
      (is (map? result))
      (is (= :data/query-result (:command/kind result)))
      (is (map? (:result result))))))

(deftest query-model-names-test
  (testing "Query model names command handler"
    (let [cmd {:command/kind :query/model-names
               :query-id 4}
          result (api/query-handler cmd)]
      (is (map? result))
      (is (= :data/query-result (:command/kind result)))
      (is (sequential? (:result result))))))

(deftest query-criteria-names-test
  (testing "Query criteria names command handler"
    (let [cmd {:command/kind :query/criteria-names
               :query-id 5}
          result (api/query-handler cmd)]
      (is (map? result))
      (is (= :data/query-result (:command/kind result)))
      (is (sequential? (:result result))))))

(deftest command-create-evaluation-test
  (testing "Create evaluation command handler"
    (let [cmd {:command/kind :command/create-evaluation
               :command-id 1
               :command/data {:id "test-eval-001"
                              :input "Test input"
                              :output "Test output"
                              :model-name "gpt-4"}}
          result (api/command-handler cmd)]
      (is (map? result))
      (is (= :data/command-result (:command/kind result)))
      (is (= 1 (:command-id result)))
      (is (get-in result [:result :success])))))

(deftest command-create-score-test
  (testing "Create score command handler"
    (let [cmd {:command/kind :command/create-score
               :command-id 2
               :command/data {:id "score-test-001"
                              :criterion-name "accuracy"
                              :score-value 8.0
                              :judge-type "human"
                              :evaluation-id "test-eval-001"}}
          result (api/command-handler cmd)]
      (is (map? result))
      (is (= :data/command-result (:command/kind result)))
      (is (get-in result [:result :success])))))
