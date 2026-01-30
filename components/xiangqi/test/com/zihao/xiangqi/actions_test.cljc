(ns com.zihao.xiangqi.actions-test
  (:require [clojure.test :refer :all]
            [com.zihao.xiangqi.actions :as actions]))

(deftest execute-action-move-test
  (testing "Routes to move action"
    (let [store (atom {:xiangqi {:selected-pos [9 2]}})
          result (actions/execute-action {:store store} nil :xiangqi/move [[9 2] [8 2]])]
      (is (vector? result)))))

(deftest execute-action-select-test
  (testing "Routes to select action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action {:store store} nil :xiangqi/select [0 0]))))))

(deftest execute-action-go-back-test
  (testing "Routes to go-back action"
    (let [root (com.zihao.xiangqi.game-tree/create-root com.zihao.xiangqi.core/state)
          zipper (com.zihao.xiangqi.game-tree/make-zipper root)
          first-state (com.zihao.xiangqi.core/move com.zihao.xiangqi.core/state [9 0] [8 0])
          updated-zipper (com.zihao.xiangqi.game-tree/add-move zipper "a0a1" first-state)
          moved-zipper (com.zihao.xiangqi.game-tree/navigate-to-move updated-zipper "a0a1")
          store (atom {:xiangqi {:game-tree moved-zipper}})]
      (is (vector? (actions/execute-action {:store store} nil :xiangqi/go-back []))))))

(deftest execute-action-restart-test
  (testing "Routes to restart action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action {:store store} nil :xiangqi/restart []))))))

(deftest execute-action-export-game-tree-test
  (testing "Routes to export-game-tree action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action {:store store} nil :xiangqi/export-game-tree []))))))

(deftest execute-action-import-game-tree-test
  (testing "Routes to import-game-tree action"
    (let [store (atom {})]
      (is (vector? (actions/execute-action {:store store} nil :xiangqi/import-game-tree []))))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})]
      (is (nil? (actions/execute-action {:store store} nil :unknown/action []))))))
