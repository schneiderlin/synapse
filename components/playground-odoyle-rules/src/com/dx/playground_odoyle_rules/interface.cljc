(ns com.dx.playground-odoyle-rules.interface
  (:require
   [com.dx.playground-odoyle-rules.core :as core]))

(def init-board
  core/init-board)

(defn select-piece [row col]
  (core/select-piece row col))

(defn move-selected-piece [to-row to-col]
  (core/move-selected-piece to-row to-col))

(defn get-board [] 
  (core/get-board))

(defn get-current-turn []
  (core/get-current-turn))

(defn get-possible-moves [] 
  (core/get-possible-moves))

(defn reset-game []
  (core/reset-game))

