(ns com.zihao.xiangqi.api
  (:require 
   [ring.util.response :as response]
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn query-handler [query]
  (case (:query/kind query)
    :query/import-game-tree
    (response/response {:success? true
                        :result (read-string (slurp (io/file "data/game_tree/game_tree.edn")))})
    nil))

(comment
  (query-handler {:query/kind :query/adb-devices})
  :rcf)

(defn command-handler [{:command/keys [kind data]}]
  (case kind
    :command/export-game-tree
    (let [{:keys [game-tree]} data]
      (spit (io/file "data/game_tree/game_tree.edn") (pr-str game-tree))
      (response/response {:success? true})) 
    nil))

(comment

  :rcf)
