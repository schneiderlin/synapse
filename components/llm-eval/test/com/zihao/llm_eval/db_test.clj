(ns com.zihao.llm-eval.db-test
  (:require [clojure.test :refer :all]
            [com.zihao.llm-eval.db :as db]))

(deftest get-evaluations-test
  (testing "Returns paginated evaluations"
    (let [result (db/get-evaluations {} 1 10 :timestamp :desc)]
      (is (map? result))
      (is (contains? result :data))
      (is (contains? result :total))
      (is (contains? result :page))
      (is (contains? result :size))
      (is (contains? result :total-pages)))))

(deftest get-evaluations-pagination-test
  (testing "Pagination works correctly"
    (let [result (db/get-evaluations {} 1 5 :timestamp :desc)]
      (is (= 1 (:page result)))
      (is (= 5 (:size result)))
      (is (<= (count (:data result)) 5)))))

(deftest get-evaluation-by-id-test
  (testing "Returns evaluation by id"
    (let [evals (db/get-evaluations {} 1 1 :timestamp :desc)
          first-id (-> evals :data first :evaluation/id)
          result (db/get-evaluation-by-id first-id)]
      (is (some? result))
      (is (= first-id (:evaluation/id result))))))

(deftest get-evaluation-stats-test
  (testing "Returns evaluation statistics"
    (let [stats (db/get-evaluation-stats)]
      (is (map? stats))
      (is (contains? stats :total-evaluations))
      (is (contains? stats :model-counts))
      (is (contains? stats :avg-scores))
      (is (contains? stats :date-range)))))

(deftest get-model-names-test
  (testing "Returns list of model names"
    (let [models (db/get-model-names)]
      (is (sequential? models))
      (is (> (count models) 0)))))

(deftest get-criteria-names-test
  (testing "Returns list of criteria names"
    (let [criteria (db/get-criteria-names)]
      (is (sequential? criteria))
      (is (> (count criteria) 0)))))

(deftest get-score-distributions-test
  (testing "Returns score distributions"
    (let [distributions (db/get-score-distributions)]
      (is (sequential? distributions))
      (is (> (count distributions) 0))
      (let [first-dist (first distributions)]
        (is (contains? first-dist :criterion-name))
        (is (contains? first-dist :judge-type))
        (is (contains? first-dist :distribution))
        (is (contains? first-dist :total))
        (is (contains? first-dist :avg))))))
