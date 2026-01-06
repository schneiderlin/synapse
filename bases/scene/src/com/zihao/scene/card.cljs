(ns com.zihao.scene.card
  (:require-macros
   [portfolio.replicant :refer [defscene]])
  (:require 
   [com.zihao.language-learn.fsrs.render :as fsrs-render]
   [com.zihao.language-learn.fsrs.template :as template]
   [portfolio.replicant :as pr]))

(defscene text-card-front
  (fsrs-render/card-ui {:card (template/text-card "front" "back")
                        :show-back false}))

(defscene text-card-back
  (fsrs-render/card-ui {:card (template/text-card "front" "back")
                        :show-back true}))

(defscene bhs-indo-card-front
  (fsrs-render/card-ui {:card (template/bhs-indo-card "anjing" "dog")
                        :show-back false}))

(defscene bhs-indo-card-back
  (fsrs-render/card-ui {:card (template/bhs-indo-card "anjing" "dog")
                        :show-back true}))

(defscene xiangqi-card-front
  (fsrs-render/card-ui {:card (template/xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1" "a0a1")
                        :show-back false}))

(defscene xiangqi-card-back
  (fsrs-render/card-ui {:card 
                        (template/xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1" 
                                               "a0a1")
                        :show-back true}))

(defscene button-neutral
  [:button {:class ["btn" "btn-neutral"]} "Neutral"])
