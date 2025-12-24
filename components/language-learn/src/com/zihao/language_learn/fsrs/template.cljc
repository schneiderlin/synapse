(ns com.zihao.language-learn.fsrs.template
  (:require 
   [linzihao.utils :refer [gen-uuid]]
   [com.zihao.language-learn.fsrs.core :as core]
   [xiangqi.common :as xqc]
   [xiangqi.fen :as fen]
   [xiangqi.render :as xiangqi-render]))

(defn text-card [front back]
  (core/create-card (gen-uuid) front back))

(defn bhs-indo-card [id-text en-text]
  (assoc (core/create-card (gen-uuid)
                    id-text
                    en-text)
         :card/id-word id-text))

(comment
  (bhs-indo-card "id" "en")
  :rcf)

(defn xiangqi-card [fen-string move-string]
  (let [xiangqi-state (fen/fen->state fen-string)
        front-state {xqc/prefix xiangqi-state}
        back-state {xqc/prefix (assoc xiangqi-state
                                      :suggested-moves
                                      [(fen/move-str->coords move-string)])}]
    (assoc (core/create-card (gen-uuid)
                             (xiangqi-render/chessboard front-state)
                             (xiangqi-render/chessboard back-state))
           :card/fen fen-string)))

(comment
  (xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1" 
                "a0a1")
  
  
  (let [fen-string "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1"
        state {xqc/prefix (fen/fen->state fen-string)}]
    (xiangqi-render/chessboard state))
  :rcf)

