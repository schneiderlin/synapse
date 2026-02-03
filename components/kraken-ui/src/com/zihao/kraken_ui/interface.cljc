(ns com.zihao.kraken-ui.interface
  (:require [com.zihao.kraken-ui.core :as core]
            [com.zihao.kraken-ui.actions :as actions]))

(defn render-board [state]
  (core/render-board state))

(defn execute-action [{:keys [store] :as _system} _e action args] 
  (actions/execute-action store action args))
