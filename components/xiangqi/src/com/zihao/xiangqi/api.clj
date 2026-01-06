(ns com.zihao.xiangqi.api
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]
   [com.zihao.xiangqi.interface :as interface]))

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

(defn ws-event-handler
  "WebSocket event handler for xiangqi component.
   Returns nil if event not handled, non-nil if handled."
  [_system {:keys [id ?data ?reply-fn]}]
  (case id
    :xiangqi/move
    (let [{:keys [from to]} ?data]
      (when ?reply-fn
        (let [new-state (interface/move interface/state from to)]
          (?reply-fn {:success? true
                     :new-state new-state}))))
    nil))
