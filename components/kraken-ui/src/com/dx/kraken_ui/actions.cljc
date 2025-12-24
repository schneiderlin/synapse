(ns com.zihao.kraken-ui.actions
  (:require
   [com.zihao.kraken-ui.common :refer [prefix]]
   [com.zihao.playground-odoyle-rules.interface :as kraken]))

(defn- update-board []
  (let [board (kraken/get-board)
        possible-moves (kraken/get-possible-moves)]
    [[:store/assoc-in [prefix :board] board]
     [:store/assoc-in [prefix :possible-moves] possible-moves]]))

(defn select-piece [row col]
  (kraken/select-piece row col)
  (update-board))

(defn move-selected-piece [to-row to-col]
  (kraken/move-selected-piece to-row to-col)
  (update-board))

(defn execute-action [store action args]
  (case action 
    :kraken/select-piece (apply select-piece args)
    :kraken/move-selected-piece (apply move-selected-piece args)
    nil))

