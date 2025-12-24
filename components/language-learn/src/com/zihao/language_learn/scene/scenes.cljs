(ns com.zihao.language-learn.scene.scenes
  (:require
   [replicant.dom :as r]
   [com.zihao.replicant-main.interface :refer [interpolate]] 
   [portfolio.ui :as portfolio]
   [com.zihao.language-learn.scene.card :as card]
   [com.zihao.language-learn.scene.chessboard :as chessboard]))

(r/set-dispatch! (fn [_ event-data] (prn event-data)))

(defn main []
  (portfolio/start!
   {:config
    {:css-paths ["/output.css"]
     :viewport/defaults
     {:background/background-color "#fdeddd"}}}))



