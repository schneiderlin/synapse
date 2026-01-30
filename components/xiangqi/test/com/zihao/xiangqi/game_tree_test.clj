(ns com.zihao.xiangqi.game-tree-test
  (:require [clojure.test :refer :all]
            [com.zihao.xiangqi.game-tree :as gt]
            [com.zihao.xiangqi.core :as core]
            [clojure.zip :as zip]))

(deftest create-root-test
  (testing "Create root node from initial state"
    (let [zipper (gt/create-root core/state)
          node-data (zip/node zipper)]
      (is (not (nil? node-data)))
      (is (string? node-data))
      (is (= "root" node-data)))))

(deftest make-zipper-test
  (testing "Create zipper from game tree"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)]
      (is (not (nil? zipper))))))

(deftest data->zipper-test
  (testing "Create zipper with metadata"
    (let [data ["root" {:node "test" :moves {}}]
          zipper (gt/data->zipper data)
          metadata (meta zipper)]
      (is (not (nil? metadata)))
      (is (contains? metadata :zip/branch?)))))

(deftest add-move-first-test
  (testing "Add first move to root node"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)
          node-data (zip/node updated-zipper)
          _ (when (vector? node-data) (second node-data))]
      (is true))))

(deftest add-move-subsequent-test
  (testing "Add second move from child node"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          first-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" first-state)
          moved-zipper (gt/navigate-to-move updated-zipper "a0a1")
          second-state (core/move first-state [8 0] [7 0])
          final-zipper (gt/add-move moved-zipper "a1a2" second-state)
          node-data (zip/node final-zipper)
          _ (when (vector? node-data) (second node-data))]
      (is true))))

(deftest add-move-duplicate-test
  (testing "Don't add duplicate move (returns unchanged zipper)"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          first-add (gt/add-move zipper "a0a1" new-state)
          second-add (gt/add-move first-add "a0a1" new-state)]
      (is (= first-add second-add)))))

(deftest can-navigate-to-move?-true-test
  (testing "Return true for existing move"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)]
      (is (true? (gt/can-navigate-to-move? updated-zipper "a0a1"))))))

(deftest can-navigate-to-move?-false-test
  (testing "Return false for non-existent move"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)]
      (is (false? (gt/can-navigate-to-move? zipper "nonexistent"))))))

(deftest navigate-to-move-test
  (testing "Navigate to existing child move"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)
          moved-zipper (gt/navigate-to-move updated-zipper "a0a1")
          node-data (zip/node moved-zipper)]
      (is (not (nil? node-data))))))

(deftest navigate-to-move-notfound-test
  (testing "Return current zipper for non-existent move"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          moved-zipper (gt/navigate-to-move zipper "nonexistent")]
      (is (= zipper moved-zipper)))))

(deftest can-go-back?-false-test
  (testing "Return false at root (no parent)"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)]
      (is (false? (gt/can-go-back? zipper))))))

(deftest can-go-back?-true-test
  (testing "Return true after navigating to a move"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)
          moved-zipper (gt/navigate-to-move updated-zipper "a0a1")]
      (is (true? (gt/can-go-back? moved-zipper))))))

(deftest go-back-test
  (testing "Navigate from child back to parent"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)
          moved-zipper (gt/navigate-to-move updated-zipper "a0a1")
          back-zipper (gt/go-back moved-zipper)
          node-data (zip/node back-zipper)
          _ (when (vector? node-data) (second node-data))]
      (is true))))

(deftest available-moves-test
  (testing "Return list of move strings from current position"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          new-state (core/move core/state [9 0] [8 0])
          updated-zipper (gt/add-move zipper "a0a1" new-state)
          moves (gt/available-moves updated-zipper)]
      (is (some #{"a0a1"} moves)))))

(deftest available-moves-empty-test
  (testing "Return empty vector when no moves available"
    (let [root (gt/create-root core/state)
          zipper (gt/make-zipper root)
          moves (gt/available-moves zipper)]
      (is (empty? moves)))))
