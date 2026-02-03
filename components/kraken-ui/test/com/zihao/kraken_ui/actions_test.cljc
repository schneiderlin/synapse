(ns com.zihao.kraken-ui.actions-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.kraken-ui.actions :as actions]))

(deftest execute-action-select-piece-test
  (testing "Routes to select-piece action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action store :kraken/select-piece [0 0]))))))

(deftest execute-action-move-selected-piece-test
  (testing "Routes to move-selected-piece action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action store :kraken/move-selected-piece [1 1]))))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})]
      (is (nil? (actions/execute-action store :unknown/action []))))))
