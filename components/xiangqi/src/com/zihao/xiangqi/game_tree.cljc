(ns com.zihao.xiangqi.game-tree
  (:require [com.zihao.xiangqi.fen :as fen]
            [clojure.zip :as zip]))

(defn- create-node
  "Create a new game tree node with FEN string"
  [state]
  {:node (fen/state->fen state)
   :moves {}})

(defn can-navigate-to-move?
  "Check if a specific move is available from current position"
  [zipper move-str]
  (let [[_move node] (zip/node zipper)]
    (contains? (:moves node) move-str)))

(defn add-move
  "Add a new move to the game tree zipper. Returns updated zipper with new move added."
  [zipper move-str new-state]
  (if (can-navigate-to-move? zipper move-str)
    zipper
    (let [new-node (create-node new-state)
          [move node] (zip/node zipper)
          _ (println "move" move "node" node "zipper" zipper "meta" (meta zipper))
          updated-node (update node :moves assoc move-str new-node)]
      (zip/replace zipper [move updated-node]))))

(defn- branch? [[_move node]]
  (and (:moves node) (not (empty? (:moves node)))))

(defn- children [[_move node]]
  (:moves node))

(defn- make-node [[move node] moves]
  [move (assoc node :moves (into {} moves))])

;; Zipper functions for navigation
(defn make-zipper
  "Create a zipper from a game tree"
  [tree]
  (zip/zipper branch? children make-node
              tree))

(defn data->zipper [data]
  (with-meta data
    {:zip/branch? branch? :zip/children children :zip/make-node make-node}))

(defn create-root
  "Create a new game tree root with FEN string"
  [state]
  (data->zipper ["root" (create-node state)]))

(comment
  (require '[com.zihao.xiangqi.interface :as logic])

  (meta (data->zipper ["1"]))

  ;; Create initial game tree
  (def initial-tree (create-root logic/state))

  ;; Create zipper
  (def z (make-zipper initial-tree))

  ;; Add a move using zipper  
  (def z-with-move
    (add-move z
              "h2e2"
              (logic/move logic/state [7 7] [7 4])))
  :rcf)

(defn navigate-to-move
  "Navigate to a specific move in the game tree using zipper"
  [zipper move-str]
  (if (can-navigate-to-move? zipper move-str)
    (if-let [child-zipper (zip/down zipper)]
      (loop [current child-zipper]
        (cond
          (zip/end? current) zipper
          (= (first (zip/node current)) move-str) current
          :else (recur (zip/right current))))
      zipper)
    zipper))  ; Return current zipper if move doesn't exist

(comment
  (can-navigate-to-move? z-with-move "h2e2")

  (def moved-z (navigate-to-move z-with-move "h2e2"))

  (def z-with-move1
    (add-move moved-z
              "h7e7"
              (logic/move logic/state [2 7] [2 4])))

  (def moved-z1 (navigate-to-move z-with-move1 "h7e7"))

  (zip/node moved-z)
  (zip/node moved-z1)

  (zip/up moved-z)
  (zip/up moved-z1)
  ;; TODO: 这里的问题, up 一次是可以的, 两次就会把 moves 变成 lazy seq
  (zip/up (zip/up moved-z1))
  :rcf)

(defn can-go-back?
  "Check if we can go back in the game tree"
  [zipper]
  (boolean (zip/up zipper)))

(comment
  ;; Test navigation and going back
  (can-go-back? z)  ; Should be false (at root)
  (can-go-back? moved-z)  ; Should be true (after navigating to a move)  ; Should go back to parent
  :rcf)

(defn go-back
  "Go back to parent node"
  [zipper]
  (zip/up zipper))

(comment
  (-> moved-z
      go-back
      zip/node)
  :rcf)

(defn available-moves
  "Get available moves from current zipper position"
  [zipper]
  (let [[_move node] (zip/node zipper)]
    (keys (:moves node))))

(comment
  (available-moves z)
  (available-moves moved-z)
  :rcf)

