(ns com.zihao.xiangqi.interface
  (:require
   #?(:clj [com.zihao.xiangqi.api :as api])
   [com.zihao.xiangqi.core :as core]))

;; API handlers
(defn query-handler
  "Query handler for xiangqi component"
  [system query]
  #?(:clj (api/query-handler system query)
     :cljs (throw (ex-info "query-handler only available in Clojure" {}))))

(defn command-handler
  "Command handler for xiangqi component"
  [system command]
  #?(:clj (api/command-handler system command)
     :cljs (throw (ex-info "command-handler only available in Clojure" {}))))

(defn ws-event-handler
  "WebSocket event handler for xiangqi component"
  [system event-msg]
  #?(:clj (api/ws-event-handler system event-msg)
     :cljs (throw (ex-info "ws-event-handler only available in Clojure" {}))))

(defn ws-event-handler-frontend
  "Frontend WebSocket event handler for xiangqi component"
  [system event-msg]
  #?(:cljs
     (let [{:keys [id ?data]} event-msg]
       (case id
         :xiangqi/game-state-update
         (let [store (:replicant/store system)]
           (swap! store assoc :xiangqi/state ?data)
           true)  ; Return truthy to indicate handled
         nil))
     :clj nil))

;; Re-export functions from internal namespaces for use by other components
(def prefix
  "The prefix key used to access xiangqi state in a nested state map"
  :xiangqi)

(defn fen->state
  "Convert a FEN string to a xiangqi state map"
  [fen-string]
  #?(:clj ((requiring-resolve 'com.zihao.xiangqi.fen/fen->state) fen-string)))

(defn move-str->coords
  "Convert a move string (e.g. 'e0e1') to coordinate pairs [[from-row from-col] [to-row to-col]]"
  [move-str]
  #?(:clj ((requiring-resolve 'com.zihao.xiangqi.fen/move-str->coords) move-str)))

(defn coords->move-str
  "Convert coordinate pairs [[from-row from-col] [to-row to-col]] to a move string (e.g. 'e0e1')"
  [coords]
  #?(:clj ((requiring-resolve 'com.zihao.xiangqi.fen/coords->move-str) coords)))

(defn state->fen
  "Convert a xiangqi state map to a FEN string"
  [state]
  #?(:clj ((requiring-resolve 'com.zihao.xiangqi.fen/state->fen) state)))

(defn chessboard
  "Render a xiangqi chessboard from state"
  [state]
  #?(:clj ((requiring-resolve 'com.zihao.xiangqi.render/chessboard) state)))
