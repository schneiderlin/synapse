(ns com.zihao.xiangqi.actions
  (:require
   [clojure.zip :as zip]
   [com.zihao.xiangqi.core :as logic]
   [com.zihao.xiangqi.common :refer [prefix]]
   [com.zihao.xiangqi.game-tree :as game-tree]
   [com.zihao.xiangqi.fen :as fen]))

(comment
  (some-> 1
          inc)
  :rcf)

(defn move [store start end]
  (let [game-state (get-in @store [prefix])
        game-tree (some-> (get-in @store [prefix :game-tree])
                          (game-tree/add-move (fen/coords->move-str [start end])
                                              (logic/move game-state start end))
                          (game-tree/navigate-to-move (fen/coords->move-str [start end])))
        actions (into [] (keep identity
                               [[:store/update-in [prefix] #(logic/move % start end)]
                                (when game-tree [:store/assoc-in [prefix :game-tree] game-tree])
                                [:store/assoc-in [prefix :selected-pos] nil]]))]
    actions))

(defn select [_store row col]
  [[:store/assoc-in [prefix :selected-pos] [row col]]])

(defn go-back [store]
  (let [game-tree (get-in @store [prefix :game-tree])
        new-game-tree (game-tree/go-back game-tree)
        [_ new-state] (zip/node new-game-tree)]
    [[:debug/print "fen" (:node new-state) "state" (fen/fen->state (:node new-state))]
     [:store/assoc-in [prefix] (fen/fen->state (:node new-state))]
     [:store/assoc-in [prefix :game-tree] new-game-tree]]))

(defn restart [_store]
  [[:store/assoc-in [prefix] logic/state]
   [:store/assoc-in [prefix :game-tree] (game-tree/make-zipper (game-tree/create-root logic/state))]])

(defn export-game-tree [store]
  (let [game-tree (get-in @store [prefix :game-tree])]
    [[:data/command {:command/kind :command/export-game-tree
                     :command/data {:game-tree game-tree}}]]))

(defn import-game-tree [_store]
  [[:data/query
    {:query/kind :query/import-game-tree}
    {:on-success (fn [result]
                   (let [game-tree (game-tree/data->zipper result)
                         _ (println "meta" (meta game-tree))
                         [_ node] (zip/node game-tree)]
                     [[:store/assoc-in [prefix] (fen/fen->state (:node node))]
                      [:store/assoc-in [prefix :game-tree] game-tree]]))}]])

(defn execute-action [{:keys [store] :as _system} _e action args]
  (case action
    :xiangqi/move (apply move store args)
    :xiangqi/select (apply select store args)
    :xiangqi/go-back (apply go-back store args)
    :xiangqi/restart (restart store)
    :xiangqi/export-game-tree (export-game-tree store)
    :xiangqi/import-game-tree (import-game-tree store)
    nil))
