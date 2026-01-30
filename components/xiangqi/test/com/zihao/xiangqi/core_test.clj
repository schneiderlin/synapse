(ns com.zihao.xiangqi.core-test
  (:require [clojure.test :as test :refer :all]
            [com.zihao.xiangqi.core :as core]))

(deftest flip-state-test
  (testing "flipping state reverses board and swaps colors"
    (let [original core/state
          flipped (core/flip-state original)]
      ;; Board should be reversed - original [0 0] becomes [9 0] after flip
      (is (= (get-in (:board original) [0 0]) :黑车))
      (is (= (get-in (:board flipped) [9 0]) :红车))
      
      ;; Original [9 0] becomes [0 0] after flip
      (is (= (get-in (:board original) [9 0]) :红车))
      (is (= (get-in (:board flipped) [0 0]) :黑车))
      
      ;; Next player should swap
      (is (= (:next original) "红"))
      (is (= (:next flipped) "黑"))
      
      ;; prev-move should be reset
      (is (nil? (:prev-move flipped))))))

(deftest pawn-move-test
  (testing "红兵 forward movement"
    (let [board (:board core/state)]
      (is (= (core/pawn-move board [6 0]) [[5 0]]))))
  
  (testing "黑卒 forward movement"
    (let [board (:board core/state)]
      (is (= (core/pawn-move board [3 0]) [[4 0]])))))

(deftest move-test
  (testing "valid move returns new state"
    (let [original core/state
          new-state (core/move original [9 0] [8 0])]
      ;; Piece should be at new position
      (is (= (get-in (:board new-state) [8 0]) :红车))
      ;; Old position should be empty
      (is (nil? (get-in (:board new-state) [9 0])))
      ;; Next player should change
      (is (= (:next new-state) "黑"))
      ;; Prev-move should be recorded
      (is (= (:prev-move new-state) {:from [9 0] :to [8 0]})))))

(deftest move-invalid-test
  (testing "invalid move returns original state"
    (let [original core/state
          new-state (core/move original [9 0] [0 0])]
      ;; State should be unchanged
      (is (= new-state original)))))
