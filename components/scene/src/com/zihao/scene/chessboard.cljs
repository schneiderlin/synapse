(ns com.zihao.scene.chessboard
  (:require
   [replicant.dom :as r]
   [com.zihao.replicant-main.replicant.utils :refer [interpolate]]
   [com.zihao.xiangqi.actions :as actions]
   [com.zihao.xiangqi.render :as render]
   [com.zihao.xiangqi.game-tree :as game-tree]
   [com.zihao.xiangqi.fen :as fen]
   [com.zihao.xiangqi.interface :as core]
   [com.zihao.xiangqi.common :refer [prefix]]
   [portfolio.replicant :as pr :refer-macros [defscene]]))

(defscene empty-board
  (render/chessboard {prefix {}}))

(defn xiangqi-state->state [xiangqi-state]
  (let [game-tree (game-tree/create-root xiangqi-state)]
    {prefix xiangqi-state #_(assoc xiangqi-state :game-tree game-tree)}))

(comment
  (-> (xiangqi-state->state core/state)
      prefix
      :game-tree
      meta)
  :rcf)

(defscene initial-board
  :params (atom (xiangqi-state->state core/state)) 
  :on-mount (fn [store] 
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions] 
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store] 
  (render/chessboard @store))

(defscene suggested-moves-board
  (render/chessboard {prefix (assoc core/state
                                    :suggested-moves
                                    [(fen/move-str->coords "h2e2")])}))

(defscene from-fen-board1
  :params (atom (xiangqi-state->state (fen/fen->state "2rakab2/9/4b4/p6N1/4p4/9/P7P/C8/9/3K5 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store] 
  (render/chessboard @store))

(defscene from-fen-board2
  :params (atom (xiangqi-state->state (fen/fen->state "1C3k1C1/1c1Pa1N2/8b/8p/p5p2/9/P1c4n1/3AB4/4A4/4K1B2 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board3
  :params (atom (xiangqi-state->state (fen/fen->state "r1bakr3/4a4/n3b4/p1p1Cc3/9/5R3/P3C4/4B1N2/4A4/2BAK3R w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store)
  )

(defscene from-fen-board4
  :params (atom (xiangqi-state->state (fen/fen->state "4k4/4a4/4ba3/9/5r2p/6R2/9/6Nc1/4A4/3AK4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store)
  )

(defscene from-fen-board5
  :params (atom (xiangqi-state->state (fen/fen->state "3aka2r/9/9/1N7/6c2/8P/9/4B4/4A4/4KA2R w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store)
  )

(defscene from-fen-board6
  :params (atom (xiangqi-state->state (fen/fen->state "1r2kab2/4a4/c1n1b1n2/p3p1p1p/9/1R7/Pc2P1PrP/2NCBCN2/4A4/2BAK3R w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board7
  :params (atom (xiangqi-state->state (fen/fen->state "4kab2/4a2C1/4bc3/2p1p3p/5np2/2P6/4P1P2/2N1B4/4Ar3/4KABR1 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board8
  :params (atom (xiangqi-state->state (fen/fen->state "2bakab1r/9/2n1c2c1/2p1p3p/1r3np2/2P6/4P1P2/1CN1B1NCP/4A4/1RBAK3R w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board9
  :params (atom (xiangqi-state->state (fen/fen->state "1rbak4/4a4/1cn1b1c2/pRp1p3p/3N5/2P5P/P8/C3BC3/4A4/2BAK4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board10
  :params (atom (xiangqi-state->state (fen/fen->state "C3kab2/4a4/2n1b3c/pR2p3p/6p2/2P1P3P/P5r2/3CB4/4A4/4KAB2 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board11
  :params (atom (xiangqi-state->state (fen/fen->state "2bakn3/4a2r1/1r2b4/pC4R1p/4N4/R8/1cP3n1P/1C7/4A4/2B1KAB2 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board12
  :params (atom (xiangqi-state->state (fen/fen->state "1rbak4/4a2N1/4b4/pC6p/9/1R7/P1P5P/4B4/4A1nn1/2BK1A3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board13
  :params (atom (xiangqi-state->state (fen/fen->state "c1rnkab2/3Ra3n/9/R8/p1p1N1b1p/4p4/P1P2r2P/N3C4/4A4/2BAK1B2 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board14
  :params (atom (xiangqi-state->state (fen/fen->state "r3k4/3P1P3/4b4/9/1R7/9/1p1pn4/9/4p4/4K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board15
  :params (atom (xiangqi-state->state (fen/fen->state "R2ckab2/4a4/7c1/3N3N1/2b1P3p/9/P2r3nP/3CB4/4A4/3AK4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board16
  :params (atom (xiangqi-state->state (fen/fen->state "9/2R1aR2r/2Nak1n2/2p2P3/9/9/9/9/2p1r4/c2K5 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board17
  :params (atom (xiangqi-state->state (fen/fen->state "3ak1b1r/4aP3/9/6pC1/5Pc2/1RB6/7p1/9/5r3/4K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board18
  :params (atom (xiangqi-state->state (fen/fen->state "2bak4/3Pa4/4b4/N8/4C4/9/9/9/3pp2p1/5K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board19
  :params (atom (xiangqi-state->state (fen/fen->state "5ab2/3kn4/2Rab4/3nP4/9/9/9/9/1r2p4/3K5 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board20
  :params (atom (xiangqi-state->state (fen/fen->state "2bk1N3/1R2R4/1n1r3c1/p3C3p/2p1p1p2/3r2P2/P1P1P3P/2N6/4A1c2/2BAK1B2 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board21
  :params (atom (xiangqi-state->state (fen/fen->state "3ak4/4aP3/9/8N/9/8C/9/9/2r2p3/4K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board22
  :params (atom (xiangqi-state->state (fen/fen->state "2ba2b2/5kN2/2n1ca3/3R3C1/9/9/9/4B4/1r7/1rB2K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board23
  :params (atom (xiangqi-state->state (fen/fen->state "1Rb1ka3/7Cr/2N1ba2n/9/9/4P4/5p3/9/3pr4/5K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board24
  :params (atom (xiangqi-state->state (fen/fen->state "CR1akabr1/R8/b3c3n/p3p3p/9/9/P3P3P/9/9/2BAKABc1 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board25
  :params (atom (xiangqi-state->state (fen/fen->state "3ak1b2/4a4/4b4/3R3N1/4P4/9/1n4r2/3AB4/4A4/2BK5 w w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board26
  :params (atom (xiangqi-state->state (fen/fen->state "R2akarr1/1NC5C/9/9/9/9/6p2/4B4/2n1p4/5K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board27
  :params (atom (xiangqi-state->state (fen/fen->state "3k2r2/4P4/9/3P5/9/9/9/4B3C/5p3/4K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board28
  :params (atom (xiangqi-state->state (fen/fen->state "2bak1r2/4a4/4b4/9/4C4/8N/P8/7R1/4p4/c2K5 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board29
  :params (atom (xiangqi-state->state (fen/fen->state "3akab2/9/c3r3b/2PP3R1/9/9/9/9/9/3K5 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board30
  :params (atom (xiangqi-state->state (fen/fen->state "3a1k3/4a4/4b4/p8/2p2r2P/P8/9/B3C2R1/4A4/3AK4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board31
  :params (atom (xiangqi-state->state (fen/fen->state "1rb1k4/4a3P/4ba3/3N1R3/9/9/P8/9/4p4/5K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board32
  :params (atom (xiangqi-state->state (fen/fen->state "2ba1k3/3Pa1R2/5rN2/2n6/9/9/9/9/6r2/C3K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store)
  )

(defscene from-fen-board33
  :params (atom (xiangqi-state->state (fen/fen->state "4ka3/2P2P3/9/9/9/9/9/5C3/3pp3p/5K3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board34
  :params (atom (xiangqi-state->state (fen/fen->state "2bCk4/3R1P1C1/4b4/p8/9/2r6/3p5/9/3KA4/1r3A3 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))

(defscene from-fen-board35
  :params (atom (xiangqi-state->state (fen/fen->state "3a2b2/4a3C/4bk3/7C1/9/2NR5/9/9/3p1r2r/4K4 w")))
  :on-mount (fn [store]
              (r/set-dispatch!
               (fn [{:keys [replicant/dom-event]} actions]
                 (->> actions
                      (interpolate dom-event)
                      (actions/execute-actions store dom-event)))))
  [store]
  (render/chessboard @store))
