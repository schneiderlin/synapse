(ns com.zihao.xiangqi.api
  (:require 
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn query-handler [_system query]
  (case (:query/kind query)
    :query/import-game-tree
    (read-string (slurp (io/file "data/game_tree/game_tree.edn")))
    nil))

(comment
  (query-handler nil {:query/kind :query/adb-devices})
  :rcf)

(defn command-handler [_system {:command/keys [kind data]}]
  (case kind
    :command/export-game-tree
    (let [{:keys [game-tree]} data]
      (spit (io/file "data/game_tree/game_tree.edn") (pr-str game-tree))
      nil) 
    nil))
