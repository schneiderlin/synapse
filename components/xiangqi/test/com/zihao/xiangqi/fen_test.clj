(ns com.zihao.xiangqi.fen-test
  (:require [clojure.test :refer :all]
            [com.zihao.xiangqi.fen :as fen]))

(deftest fen->state-test
  (testing "Parse standard initial position FEN string"
    (let [fen-str "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"
          state (fen/fen->state fen-str)]
      (is (not (nil? (:board state))))
      (is (= (:next state) "红"))
      (is (nil? (:prev-move state))))))

(deftest fen->state-board-test
  (testing "Verify board layout is parsed correctly"
    (let [fen-str "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"
          state (fen/fen->state fen-str)
          board (:board state)]
      (is (= (get-in board [0 0]) :黑车))
      (is (= (get-in board [0 4]) :黑将))
      (is (= (get-in board [2 1]) :黑炮))
      (is (= (get-in board [6 0]) :红兵))
      (is (= (get-in board [7 1]) :红炮))
      (is (= (get-in board [9 0]) :红车))
      (is (= (get-in board [9 4]) :红帅)))))

(deftest fen->state-next-test
  (testing "Verify next player is parsed correctly"
    (let [fen-str-white "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1"
          fen-str-black "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1"]
      (is (= (:next (fen/fen->state fen-str-white)) "红"))
      (is (= (:next (fen/fen->state fen-str-black)) "黑")))))

(deftest state->fen-test
  (testing "Convert initial state to FEN string"
    (let [state {:board [[:黑车 :黑马 :黑象 :黑士 :黑将 :黑士 :黑象 :黑马 :黑车]
                         [nil nil nil nil nil nil nil nil nil]
                         [nil :黑炮 nil nil nil nil nil :黑炮 nil]
                         [:黑卒 nil :黑卒 nil :黑卒 nil :黑卒 nil :黑卒]
                         [nil nil nil nil nil nil nil nil nil]
                         [nil nil nil nil nil nil nil nil nil]
                         [:红兵 nil :红兵 nil :红兵 nil :红兵 nil :红兵]
                         [nil :红炮 nil nil nil nil nil :红炮 nil]
                         [nil nil nil nil nil nil nil nil nil]
                         [:红车 :红马 :红相 :红士 :红帅 :红士 :红相 :红马 :红车]]
                 :next "红"
                 :prev-move nil}
          fen-str (fen/state->fen state)]
      (is (string? fen-str))
      (is (.contains fen-str "rnbakabnr"))
      (is (.contains fen-str "RNBAKABNR"))
      (is (.contains fen-str " w ")))))

(deftest fen-roundtrip-test
  (testing "Round-trip conversion: state -> FEN -> state produces equivalent state"
    (let [original-state {:board [[:黑车 :黑马 :黑象 :黑士 :黑将 :黑士 :黑象 :黑马 :黑车]
                                  [nil nil nil nil nil nil nil nil nil]
                                  [nil :黑炮 nil nil nil nil nil :黑炮 nil]
                                  [:黑卒 nil :黑卒 nil :黑卒 nil :黑卒 nil :黑卒]
                                  [nil nil nil nil nil nil nil nil nil]
                                  [nil nil nil nil nil nil nil nil nil]
                                  [:红兵 nil :红兵 nil :红兵 nil :红兵 nil :红兵]
                                  [nil :红炮 nil nil nil nil nil :红炮 nil]
                                  [nil nil nil nil nil nil nil nil nil]
                                  [:红车 :红马 :红相 :红士 :红帅 :红士 :红相 :红马 :红车]]
                          :next "红"
                          :prev-move nil}
          fen-str (fen/state->fen original-state)
          roundtrip-state (fen/fen->state fen-str)]
      (is (= (:board roundtrip-state) (:board original-state)))
      (is (= (:next roundtrip-state) (:next original-state))))))

(deftest move-str->coords-test
  (testing "Convert 'e0e1' to [[9,4] [8,4]]"
    (let [coords (fen/move-str->coords "e0e1")]
      (is (= coords [[9 4] [8 4]])))))

(deftest move-str->coords-edges-test
  (testing "Convert corner positions"
    (is (= (fen/move-str->coords "a0a1") [[9 0] [8 0]]))
    (is (= (fen/move-str->coords "i9i8") [[0 8] [1 8]]))
    (is (= (fen/move-str->coords "e4e5") [[5 4] [4 4]]))))

(deftest coords->move-str-test
  (testing "Convert [[9,4] [8,4]] to 'e0e1'"
    (let [move-str (fen/coords->move-str [[9 4] [8 4]])]
      (is (= move-str "e0e1")))))

(deftest coords->move-str-roundtrip-test
  (testing "Round-trip: coords -> move-str -> coords produces same coordinates"
    (let [original-coords [[9 4] [8 4]]
          move-str (fen/coords->move-str original-coords)
          roundtrip-coords (fen/move-str->coords move-str)]
      (is (= roundtrip-coords original-coords)))
    (let [original-coords [[0 0] [1 1]]
          move-str (fen/coords->move-str original-coords)
          roundtrip-coords (fen/move-str->coords move-str)]
      (is (= roundtrip-coords original-coords)))))
