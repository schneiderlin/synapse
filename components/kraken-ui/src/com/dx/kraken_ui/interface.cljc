(ns com.dx.kraken-ui.interface
  (:require [com.dx.kraken-ui.core :as core]
            [com.dx.kraken-ui.actions :as actions]))

(defn render-board [state]
  (core/render-board state))

(defn execute-action [{:keys [store] :as system} _e action args] 
  (actions/execute-action store action args))
