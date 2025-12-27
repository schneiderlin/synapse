(ns com.zihao.language-learn.fsrs.template
  (:require
   [com.zihao.replicant-main.interface :as replicant]
   [com.zihao.language-learn.fsrs.core :as core]
   [com.zihao.xiangqi.interface :as xiangqi]))

(defn text-card [front back]
  (core/create-card (replicant/gen-uuid) front back))

(defn bhs-indo-card [id-text en-text]
  (assoc (core/create-card (replicant/gen-uuid)
                           id-text
                           en-text)
         :card/id-word id-text))

(comment
  (bhs-indo-card "id" "en")
  :rcf)

(defn xiangqi-card [fen-string move-string]
  (let [xiangqi-state (xiangqi/fen->state fen-string)
        front-state {xiangqi/prefix xiangqi-state}
        back-state {xiangqi/prefix (assoc xiangqi-state
                                          :suggested-moves
                                          [(xiangqi/move-str->coords move-string)])}]
    (assoc (core/create-card (replicant/gen-uuid)
                             (xiangqi/chessboard front-state)
                             (xiangqi/chessboard back-state))
           :card/fen fen-string)))

(comment
  (xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1"
                "a0a1")

  (let [fen-string "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR b - - 0 1"
        state {xiangqi/prefix (xiangqi/fen->state fen-string)}]
    (xiangqi/chessboard state))
  :rcf)

