(ns com.zihao.xiangqi.fen
  (:require [clojure.string :as str]
            [com.zihao.replicant-main.interface :as replicant]))

(def piece-map
  {"k" :黑将, "a" :黑士, "b" :黑象, "n" :黑马, "r" :黑车, "c" :黑炮, "p" :黑卒
   "K" :红帅, "A" :红士, "B" :红相, "N" :红马, "R" :红车, "C" :红炮, "P" :红兵})

(defn fen-row->board-row [row]
  (loop [chars (seq row)
         acc []]
    (if (empty? chars)
      acc
      (let [c (first chars)]
        (if (replicant/is-digit? c)
          (recur (rest chars) (into acc (repeat (replicant/parse-int c) nil)))
          (recur (rest chars) (conj acc (piece-map (str c)))))))))

(comment
  (replicant/parse-int "c")
  (replicant/is-digit? "c")
  (fen-row->board-row "1c5c1")

  (seq "1c5c1")
  (replicant/parse-int "5")
  (repeat 5 nil)
  :rcf)

(defn fen->state [fen]
  (let [[board-part turn & _] (str/split fen #" ")
        rows (str/split board-part #"/")
        board (vec (map fen-row->board-row rows))
        next (case turn
               "b" "黑"
               "w" "红")]
    {:board board
     :next next
     :prev-move nil}))

(def file->col
  {\a 0 \b 1 \c 2 \d 3 \e 4 \f 5 \g 6 \h 7 \i 8})

(def col->file
  {0 \a 1 \b 2 \c 3 \d 4 \e 5 \f 6 \g 7 \h 8 \i})

(defn move-str->coords [move-str]
  (if (nil? move-str)
    nil
    (let [[from to] [(subs move-str 0 2) (subs move-str 2 4)]
          [from-file from-rank] (seq from)
          [to-file to-rank] (seq to)
          rank->row #(case %
                       \0 9
                       \1 8
                       \2 7
                       \3 6
                       \4 5
                       \5 4
                       \6 3
                       \7 2
                       \8 1
                       \9 0)]
      [[(rank->row from-rank) (file->col from-file)]
       [(rank->row to-rank) (file->col to-file)]])))

(defn coords->move-str [coords]
  (if (nil? coords)
    nil
    (let [[[from-row from-col] [to-row to-col]] coords
          row->rank #(case %
                       9 \0
                       8 \1
                       7 \2
                       6 \3
                       5 \4
                       4 \5
                       3 \6
                       2 \7
                       1 \8
                       0 \9)]
      (str (col->file from-col)
           (row->rank from-row)
           (col->file to-col)
           (row->rank to-row)))))

(comment
  (move-str->coords nil)
  (move-str->coords "i1i0")
  (coords->move-str [[8 8] [9 8]])

  (move-str->coords "e0e1")
  (coords->move-str [[9 4] [8 4]])

  (coords->move-str nil)
  (coords->move-str [[9 8] [8 8]])
  (coords->move-str [[4 4] [5 4]])

  :rcf)

(def piece-rev-map
  {:黑将 "k", :黑士 "a", :黑象 "b", :黑马 "n", :黑车 "r", :黑炮 "c", :黑卒 "p"
   :红帅 "K", :红士 "A", :红相 "B", :红马 "N", :红车 "R", :红炮 "C", :红兵 "P"})

(defn board-row->fen-row [row]
  (let [f (fn [acc x]
            (if x
              (conj acc (piece-rev-map x))
              (if (and (seq acc) (re-matches #"\d+" (last acc)))
                (conj (pop acc) (str (inc (replicant/parse-int (last acc)))))
                (conj acc "1"))))]
    (apply str (reduce f [] row))))

(defn state->fen
  "Convert {:board .. :next ..} state to FEN string."
  [{:keys [board next]}]
  (let [rows (map board-row->fen-row board)
        turn (if (= next "红") "w" "b")]
    (str (clojure.string/join "/" rows) " " turn " - - 0 1")))

(comment
  (require '[com.zihao.xiangqi.interface :as logic])
  (-> logic/state
      state->fen
      fen->state)

  (fen->state "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w - - 0 1")
  :rcf)

