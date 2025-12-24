(ns com.zihao.scene.scenes
  (:require
   [replicant.dom :as r]
   [portfolio.ui :as portfolio]
   [com.zihao.scene.card :as card]
   [com.zihao.scene.chessboard :as chessboard]))

(r/set-dispatch! (fn [_ event-data] (prn event-data)))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/output.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))



