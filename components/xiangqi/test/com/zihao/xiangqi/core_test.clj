(ns com.zihao.xiangqi.core-test
  (:require [clojure.test :refer [deftest is testing]]
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

(deftest cannon-move-test
  (testing "Red cannon moves from initial position [7,1]"
    (let [board (:board core/state)
          moves (core/cannon-move board [7 1])]
      (is (some #{[6 1]} moves))
      (is (some #{[5 1]} moves))
      (is (some #{[4 1]} moves))
      (is (some #{[8 1]} moves))
      (is (some #{[7 0]} moves))
      (is (some #{[7 2]} moves)))))

(deftest cannon-move-black-test
  (testing "Black cannon moves from initial position [2,1]"
    (let [board (:board core/state)
          moves (core/cannon-move board [2 1])]
      (is (some #{[1 1]} moves))
      (is (some #{[3 1]} moves))
      (is (some #{[4 1]} moves))
      (is (some #{[2 0]} moves))
      (is (some #{[2 2]} moves)))))

(deftest chariot-move-test
  (testing "Red chariot moves horizontally/vertically from [9,0]"
    (let [board (:board core/state)
          moves (core/chariot-move board [9 0])]
      (is (some #{[8 0]} moves))
      (is (some #{[7 0]} moves)))))

(deftest chariot-move-black-test
  (testing "Black chariot moves horizontally/vertically from [0,0]"
    (let [board (:board core/state)
          moves (core/chariot-move board [0 0])]
      (is (some #{[1 0]} moves))
      (is (some #{[2 0]} moves)))))

(deftest knight-move-test
  (testing "Red knight moves from [9,1] with valid positions"
    (let [board (:board core/state)
          moves (core/knight-move board [9 1])]
      (is (some #{[7 0]} moves))
      (is (some #{[7 2]} moves)))))

(deftest knight-move-black-test
  (testing "Black knight moves from [0,1] with valid positions"
    (let [board (:board core/state)
          moves (core/knight-move board [0 1])]
      (is (some #{[2 0]} moves))
      (is (some #{[2 2]} moves)))))

(deftest bishop-move-red-test
  (testing "Red elephant moves within territory (rows 5-9)"
    (let [board (:board core/state)
          moves (core/bishop-move board [9 2])]
      (is (some #{[7 0]} moves))
      (is (some #{[7 4]} moves)))))

(deftest bishop-move-black-test
  (testing "Black elephant moves within territory (rows 0-4)"
    (let [board (:board core/state)
          moves (core/bishop-move board [0 2])]
      (is (some #{[2 0]} moves))
      (is (some #{[2 4]} moves)))))

(deftest advisor-move-red-test
  (testing "Red advisor moves within palace (rows 7-9, cols 3-5)"
    (let [board (:board core/state)
          moves (core/advisor-move board [9 3])]
      (is (some #{[8 4]} moves)))))

(deftest advisor-move-black-test
  (testing "Black advisor moves within palace (rows 0-2, cols 3-5)"
    (let [board (:board core/state)
          moves (core/advisor-move board [0 3])]
      (is (some #{[1 4]} moves)))))

(deftest general-move-red-test
  (testing "Red general moves within palace"
    (let [board (:board core/state)
          moves (core/general-move board [9 4])]
      (is (some #{[8 4]} moves)))))

(deftest general-move-black-test
  (testing "Black general moves within palace"
    (let [board (:board core/state)
          moves (core/general-move board [0 4])]
      (is (some #{[1 4]} moves)))))

(deftest possible-move-pawn-test
  (testing "Returns valid pawn moves"
    (let [moves (core/possible-move core/state [6 0])]
      (is (some #{[5 0]} moves)))))

(deftest possible-move-chariot-test
  (testing "Returns valid chariot moves"
    (let [moves (core/possible-move core/state [9 0])]
      (is (some #{[8 0]} moves)))))

(deftest possible-move-knight-test
  (testing "Returns valid knight moves"
    (let [moves (core/possible-move core/state [9 1])]
      (is (some #{[7 0]} moves)))))

(deftest possible-move-cannon-test
  (testing "Returns valid cannon moves"
    (let [moves (core/possible-move core/state [7 1])]
      (is (some #{[6 1]} moves)))))

(deftest possible-move-bishop-test
  (testing "Returns valid elephant moves"
    (let [moves (core/possible-move core/state [9 2])]
      (is (some #{[7 0]} moves)))))

(deftest possible-move-advisor-test
  (testing "Returns valid advisor moves"
    (let [moves (core/possible-move core/state [9 3])]
      (is (some #{[8 4]} moves)))))

(deftest possible-move-general-test
  (testing "Returns valid general moves"
    (let [moves (core/possible-move core/state [9 4])]
      (is (some #{[8 4]} moves)))))
