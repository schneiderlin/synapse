(ns com.zihao.xiangqi.interface
  (:require
   #?(:clj [com.zihao.xiangqi.api :as api])))

(def state
  {:board [[:黑车 :黑马 :黑象 :黑士 :黑将 :黑士 :黑象 :黑马 :黑车]
           [nil nil nil nil nil nil nil nil nil]
           [nil :黑炮 nil nil nil nil nil :黑炮 nil]
           [:黑卒 nil :黑卒 nil :黑卒 nil :黑卒 nil :黑卒]
           [nil nil nil nil nil nil nil nil nil]
           [nil nil nil nil nil nil nil nil nil]
           [:红兵 nil :红兵 nil :红兵 nil :红兵 nil :红兵]
           [nil :红炮 nil nil nil nil nil :红炮 nil]
           [nil nil nil nil nil nil nil nil nil]
           [:红车 :红马 :红相 :红士 :红帅 :红士 :红相 :红马 :红车]]
   :next "红"
   :prev-move nil})

(defn flip-state
  "红黑调换
   行列都 reverse, 颜色也 reverse, next 也是"
  [state]
  (let [reversed-board
        (->> (:board state)
             reverse  ; Reverse row
             (map reverse)  ; Reverse column
             (map (fn [row]  ; Reverse piece
                    (map (fn [piece]
                           (when piece
                             ({:黑车 :红车
                               :黑马 :红马
                               :黑炮 :红炮
                               :黑象 :红相
                               :黑士 :红士
                               :黑将 :红帅
                               :黑卒 :红兵

                               :红车 :黑车
                               :红马 :黑马
                               :红炮 :黑炮
                               :红相 :黑象
                               :红士 :黑士
                               :红帅 :黑将
                               :红兵 :黑卒}
                              piece)))
                         row))))]
    (assoc state
           :board reversed-board
           :next (if (= "红" (:next state)) "黑" "红")
           :prev-move nil)))

(comment
  (flip-state state)
  :rcf)

(defn pawn-move
  "同时处理红兵和黑卒的移动规则"
  [board [row col]]
  (let [piece (get-in board [row col])
        ; 红兵和黑卒的参数分开定义
        [forward-delta cross-river? enemy-pieces]
        (cond
          (= :红兵 piece) [-1 #(<= % 4) #{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒}]
          (= :黑卒 piece) [+1 #(>= % 5) #{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵}]
          :else [nil nil nil])

        ; 计算前进位置
        forward-row (when forward-delta (+ row forward-delta))
        valid-forward (when (and forward-row (<= 0 forward-row 9))
                        [forward-row col])

        ; 过河后可以横向移动
        valid-side (when (and forward-delta (cross-river? row))
                     (->> [[row (dec col)] [row (inc col)]]
                          (filter (fn [[_ c]] (<= 0 c 8)))
                          (filter (fn [[r c]] (let [target (get-in board [r c])]
                                                (or (nil? target) (enemy-pieces target)))))))]

    (cond-> []
      valid-forward (conj valid-forward)
      valid-side (into valid-side))))

(comment
  ;; 红
  (pawn-move (:board state) [5 0])
  (pawn-move (:board state) [5 2])
  (pawn-move (:board state) [5 4])
  (pawn-move (:board state) [5 6])
  (pawn-move (:board state) [5 8])

  ;; 黑
  (pawn-move (:board state) [3 0])
  (pawn-move (:board state) [3 2])
  (pawn-move (:board state) [3 4])
  (pawn-move (:board state) [3 6])
  (pawn-move (:board state) [3 8])
  :rcf)

(defn cannon-move
  "输入棋盘和起始位置，返回炮所有合法移动/吃子位置（同时处理红黑方）"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? is-cannon?]
        (cond
          (= :红炮 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} true]
          (= :黑炮 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} true]
          :else [nil false])
        directions [[-1 0] [1 0] [0 -1] [0 1]]  ; 上下左右四个方向

        valid-moves (when is-cannon?
                      (mapcat (fn [[dr dc]]
                                (loop [r (+ row dr) c (+ col dc)
                                       moves []
                                       jumped? false]
                                  (cond
                                    (or (< r 0) (> r 9) (< c 0) (> c 8)) moves

                                    (not jumped?)
                                    (if-let [target (get-in board [r c])]
                                      (recur (+ r dr) (+ c dc) moves true)  ; 遇到第一个棋子，开始寻找跳跃目标
                                      (recur (+ r dr) (+ c dc) (conj moves [r c]) false))

                                    :else  ; 跳跃模式
                                    (if-let [target (get-in board [r c])]
                                      (if (enemy? target)
                                        (conj moves [r c])  ; 找到可吃目标
                                        moves)              ; 遇到己方棋子停止
                                      (recur (+ r dr) (+ c dc) moves true)))))
                              directions))]
    (filter some? valid-moves)))

(comment
  ;; 红
  (cannon-move (:board state) [7 1])
  (cannon-move (:board state) [7 7])

  ;; 黑
  (cannon-move (:board state) [2 1])
  (cannon-move (:board state) [2 7])
  :rcf)

(defn chariot-move
  "输入棋盘和起始位置，返回车所有合法移动/吃子位置（同时处理红黑方）"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? is-chariot?] (cond
                               (= :红车 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} true]
                               (= :黑车 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} true]
                               :else [nil false])
        directions [[-1 0] [1 0] [0 -1] [0 1]]  ; 上下左右

        valid-moves (when is-chariot?
                      (mapcat (fn [[dr dc]]
                                (loop [r (+ row dr) c (+ col dc)
                                       moves []]
                                  (cond
                                    (or (< r 0) (> r 9) (< c 0) (> c 8)) moves

                                    :else
                                    (let [current (get-in board [r c])]
                                      (cond
                                        (nil? current) (recur (+ r dr) (+ c dc) (conj moves [r c]))
                                        (enemy? current) (conj moves [r c])
                                        :else moves)))))  ; 遇到己方棋子停止
                              directions))]
    valid-moves))

(defn knight-move
  "输入棋盘和起始位置，返回马可移动/吃子的位置（同时处理红黑方）"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? is-knight?] (cond
                              (= :红马 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} true]
                              (= :黑马 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} true]
                              :else [nil false])

        ; 八个可能的移动方向（先直两步再转直角）
        directions [[[1 0]  [2 1]]   ; 下右
                    [[1 0]  [2 -1]]  ; 下左
                    [[-1 0] [-2 1]]  ; 上右
                    [[-1 0] [-2 -1]] ; 上左
                    [[0 1]  [1 2]]   ; 右下
                    [[0 1]  [-1 2]]  ; 右上
                    [[0 -1] [1 -2]]  ; 左下
                    [[0 -1] [-1 -2]]] ; 左上

        valid-moves (when is-knight?
                      (->> directions
                           (keep (fn [[[dr1 dc1] [dr2 dc2]]]
                                   (let [block-point [(+ row dr1) (+ col dc1)]
                                         target-point [(+ row dr2) (+ col dc2)]]
                                     ; 检查：1. 不别马腿 2. 目标在棋盘内
                                     (when (and (not (get-in board block-point))
                                                (<= 0 (target-point 0) 9)
                                                (<= 0 (target-point 1) 8))
                                       ; 检查目标位置是否可到达（空或敌方）
                                       (let [target-piece (get-in board target-point)]
                                         (when (or (nil? target-piece) (enemy? target-piece))
                                           target-point))))))
                           (filter some?)))]
    valid-moves))

(comment
  ;; 应该可以有 上左, 上右两个走法
  (knight-move (:board state) [9 1])
  (knight-move (:board state) [9 7])

  (knight-move (:board state) [0 7])
  (knight-move (:board state) [0 7])

  (let [board (:board state)
        [row col] [9 1]
        piece (get-in board [row col])

        [enemy? is-knight?] (cond
                              (= :红马 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} true]
                              (= :黑马 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} true]
                              :else [nil false])

        [[dr1 dc1] [dr2 dc2]] [[-1 0] [-2 1]]
        block-point [(+ row dr1) (+ col dc1)]
        target-point [(+ row dr2) (+ col dc2)]
        check1 (and (not (get-in board block-point))
                    (<= 0 (target-point 0) 9)
                    (<= 0 (target-point 1) 8))
        check2 (when-let [target-piece (get-in board target-point)]
                 (or (nil? target-piece) (enemy? target-piece)))]
    check2)
  :rcf)

(defn bishop-move
  "输入棋盘和起始位置，返回象/相可移动/吃子的位置（处理红黑方）"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? river-test] (cond
                              (= :红相 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} #(>= % 5)]  ; 红相不过河（行号≥5）
                              (= :黑象 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} #(<= % 4)]  ; 黑象不过河（行号≤4）
                              :else [nil nil])

        directions [[[-1 -1] [-2 -2]]  ; 左上
                    [[-1 1]  [-2 2]]   ; 右上
                    [[1 -1]  [2 -2]]    ; 左下
                    [[1 1]   [2 2]]]    ; 右下

        valid-moves (when enemy?  ; 当是象/相时处理
                      (->> directions
                           (keep (fn [[[dr-mid dc-mid] [dr-target dc-target]]]
                                   (let [mid-point [(+ row dr-mid) (+ col dc-mid)]
                                         target-point [(+ row dr-target) (+ col dc-target)]
                                         [tr, tc] target-point]
                                     ; 检查条件：1. 不塞象眼 2. 目标在棋盘内 3. 不过河
                                     (when (and (not (get-in board mid-point))
                                                (<= 0 tr 9) (<= 0 tc 8)
                                                (river-test tr))  ; 使用river-test检查目标行
                                       (let [target-piece (get-in board [tr tc])]
                                         (when (or (nil? target-piece) (enemy? target-piece))
                                           [tr tc]))))))
                           (filter some?)))]
    valid-moves))

(comment
  (bishop-move (:board state) [9 2])
  (bishop-move (:board state) [9 6])

  (bishop-move (:board state) [0 2])
  (bishop-move (:board state) [0 6])
  :rcf)

(defn advisor-move
  "输入棋盘和起始位置，返回士可移动/吃子的位置（处理红黑方）"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? palace-bounds]
        (cond
          (= :红士 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒}
                         {:rows #{7 8 9} :cols #{3 4 5}}]
          (= :黑士 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵}
                         {:rows #{0 1 2} :cols #{3 4 5}}]
          :else [nil nil])

        directions [[-1 -1] [-1 1] [1 -1] [1 1]]  ; 四个斜方向

        valid-moves (when enemy?
                      (->> directions
                           (map (fn [[dr dc]] [(+ row dr) (+ col dc)]))
                           (filter (fn [[r c]]
                                     (and (contains? (:rows palace-bounds) r)
                                          (contains? (:cols palace-bounds) c)
                                          (let [target (get-in board [r c])]
                                            (or (nil? target) (enemy? target))))))))]
    valid-moves))

(comment
  (advisor-move (:board state) [9 3])
  (advisor-move (:board state) [9 5])

  (advisor-move (:board state) [0 3])
  (advisor-move (:board state) [0 5])
  :rcf)

(defn general-move
  "输入棋盘和起始位置，返回将/帅可移动/吃子的位置"
  [board [row col]]
  (let [piece (get-in board [row col])
        [enemy? palace-rows] (cond
                               (= :红帅 piece) [#{:黑车 :黑马 :黑象 :黑士 :黑将 :黑炮 :黑卒} #{7 8 9}]
                               (= :黑将 piece) [#{:红车 :红马 :红相 :红士 :红帅 :红炮 :红兵} #{0 1 2}]
                               :else [nil nil])

        directions [[-1 0] [1 0] [0 -1] [0 1]]  ; 上下左右

        basic-moves (when enemy?
                      (->> directions
                           (map (fn [[dr dc]] [(+ row dr) (+ col dc)]))
                           (filter (fn [[r c]]
                                     (and (contains? palace-rows r)
                                          (<= 3 c 5)
                                          (let [target (get-in board [r c])]
                                            (or (nil? target) (enemy? target))))))))

        ; 检查对脸的特殊情况
        enemy-general-col (->> (for [r (if (= :红帅 piece) (range 0 3) (range 7 10))  ; 根据己方将的位置搜索对方将
                                     c (range 3 6)]
                                 (when (= (if (= :红帅 piece) :黑将 :红帅) (get-in board [r c])) c))
                               (filter some?)
                               first)
        face-to-face? (and enemy-general-col
                           (= col enemy-general-col)
                           (empty? (filter some? (map #(get-in board [% col])
                                                      (if (= :红帅 piece)
                                                        (range (inc row) 3 -1)    ; 红帅向上搜索
                                                        (range (dec row) 7 1))))))] ; 黑将向下搜索

    (cond-> basic-moves
      face-to-face? (conj [(if (= :红帅 piece) (- row 3) (+ row 3)) col]))))

(comment
  ;; TODO 将帅对脸还有 bug
  (general-move (:board state) [9 4])
  (general-move (:board state) [0 4])
  :rcf)

(defn possible-move
  "输入棋盘和起始位置, 返回所有合法移动位置"
  [state [row col]]
  (let [board (:board state)
        piece (get-in board [row col])
        raw-moves (case piece
                    (:红兵 :黑卒) (pawn-move board [row col])
                    (:红车 :黑车) (chariot-move board [row col])
                    (:红马 :黑马) (knight-move board [row col])
                    (:红炮 :黑炮) (cannon-move board [row col])
                    (:红相 :黑象) (bishop-move board [row col])
                    (:红士 :黑士) (advisor-move board [row col])
                    (:红帅 :黑将) (general-move board [row col])
                    nil)]
    raw-moves))

(comment
  ;; 测试红车初始位置移动
  (possible-move state [9 0])
  ;; 测试红炮移动
  (possible-move state [7 1])
  :rcf)

(defn move
  "返回新游戏状态，包含：
  - 更新后的棋盘
  - 切换下棋方
  - 记录上一步移动
  若移动不合法则返回原状态"
  [state start end]
  (println "start" start "end" end)
  (let [[start-row start-col] start
        [end-row end-col] end
        current-piece (get-in (:board state) start)
        valid-moves (possible-move state start)]

    (if (some #{end} valid-moves)  ; 检查目标位置是否在合法移动列表中
      (-> state
          ; 1. 更新棋盘状态
          (assoc-in [:board end-row end-col] current-piece)
          (assoc-in [:board start-row start-col] nil)

          ; 2. 切换下棋方（红 ↔ 黑）
          (assoc :next (if (= "红" (:next state)) "黑" "红"))

          ; 3. 记录移动历史（用于悔棋/动画等功能）
          (assoc :prev-move {:from start :to end}))

      ; 非法移动时返回原状态
      state)))

(comment
  ;; 测试红车移动
  (-> state
      (move [9 0] [7 0])
      :board
      (get-in [7 0]))

  ;; 测试非法移动（象不能过河）
  (-> state
      (move [9 2] [7 4])  ; 尝试移动红相
      :board
      (get-in [7 4]))  ; 应该返回 nil
  :rcf)

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
