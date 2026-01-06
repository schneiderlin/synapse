(ns com.zihao.xiangqi.render
  (:require
   [com.zihao.xiangqi.core :as logic]
   [com.zihao.xiangqi.common :refer [prefix]]
   [com.zihao.xiangqi.game-tree :as game-tree]
   [com.zihao.xiangqi.fen :as fen]))

(defn chess-piece [{:keys [piece selected? row col next-player]}]
  (let [can-select? (= (subs (name piece) 0 1) next-player)]
    [:div
     {:class (concat ["w-24" "h-24" "flex" "items-center" "justify-center" "rounded-full" "border-8" "select-none" "transition-colors"]
                     (if selected? ["border-primary"] ["border-base-300"])
                     (if (= "红" (subs (name piece) 0 1))
                       ["text-red-600" "bg-white"]
                       ["text-neutral-800" "bg-white"])
                     (if can-select?
                       ["cursor-pointer" "hover:bg-neutral-100"]
                       ["cursor-not-allowed"]))
      :on {:click (when can-select? 
                    [[:xiangqi/select row col]])}} 
     [:span {:class ["text-5xl" "font-bold"]}
      (last (name piece))]]))

(comment
  (assoc-in {} [:selected-pos] [1 2])
  :rcf)

(defn chessboard [state]
  (let [xiangqi-state (prefix state)
        board (:board xiangqi-state)
        selected-pos (:selected-pos xiangqi-state)
        debug-pos (:debug-pos xiangqi-state)
        next-player (:next xiangqi-state)
        game-tree (get-in xiangqi-state [:game-tree])
        ;; Get possible moves from game tree if available, otherwise fall back to logic
        possible-moves (if selected-pos
                         (logic/possible-move xiangqi-state selected-pos)
                         [])
        ;; Get potential moves from current game tree node (both start and end positions)
        game-tree-moves (if game-tree
                          (let [available-move-strs (game-tree/available-moves game-tree)]
                            (->> available-move-strs
                                 (map fen/move-str->coords)
                                 (filter some?)))
                          []) 
        suggested-moves (get xiangqi-state :suggested-moves []) ;; list of coords [[start-pos end-pos]]
        ]
    [:div {:class ["relative" "w-[900px]" "h-[1200px]"]
           :style {:background-image "url('/image/Xiangqi_board.svg')"
                   :background-size "contain"
                   :background-repeat "no-repeat"
                   :background-position "center"}}

     ;; 棋子
     (for [row (range 10)]
       (for [col (range 9)]
         (let [piece (get-in board [row col])
               top (+ 100 (* row 100))
               left (* col 100)]
           (when piece
             [:div {:class ["absolute"]
                    :style {:transform (str "translate(" left "px, " top "px)")}}
              (chess-piece
               {:piece piece
                :selected? (= selected-pos [row col])
                :row row
                :col col
                :next-player next-player})]))))

     ;; Highlight possible moves first (under pieces)
     (for [move possible-moves]
       (let [[row col] move
             top (+ 100 (* row 100))
             left (* col 100)]
         [:div {:class ["absolute" "w-24" "h-24" "bg-green-500/20" "rounded-full"]
                :style {:transform (str "translate(" left "px, " top "px)")}
                :on {:click (when (and selected-pos
                                       (= (subs (name (get-in board selected-pos)) 0 1) next-player))
                              [[:xiangqi/move selected-pos [row col]]])}}]))

     ;; Add debug position highlight
     (when-let [[row col] debug-pos]
       (let [top (+ 100 (* row 100))
             left (* col 100)]
         [:div
          {:class ["absolute" "w-24" "h-24" "bg-blue-500/20" "rounded-full"]
           :style {:transform (str "translate(" left "px, " top "px)")}}]))

     ;; SVG arrows for game tree moves (rendered on top)
     [:svg {:class ["absolute" "pointer-events-none"] :style {:left "0px" :top "0px" :width "900px" :height "1200px" :z-index "10"}}
      [:defs
       [:marker {:id "arrowhead" :markerWidth "10" :markerHeight "7"
                 :refX "9" :refY "3.5" :orient "auto"}
        [:polygon {:points "0 0, 10 3.5, 0 7" :fill "#3b82f6"}]]]
      ;; game-tree and suggested moves move arrows
      (for [move-coords (concat game-tree-moves suggested-moves)]
        (let [[[start-row start-col] [end-row end-col]] move-coords
              start-top (+ 100 (* start-row 100))
              start-left (* start-col 100)
              end-top (+ 100 (* end-row 100))
              end-left (* end-col 100)]
          [:line {:x1 (+ start-left 50) :y1 (+ start-top 50)
                  :x2 (+ end-left 50) :y2 (+ end-top 50)
                  :stroke "#3b82f6" :stroke-width "3"
                  :marker-end "url(#arrowhead)"}]))]]))

(defn control-panel [state]
  (let [xiangqi-state (prefix state)
        next-player (:next xiangqi-state)]
    [:div {:class ["card" "bg-base-100" "shadow-xl" "w-full" "lg:w-80" "h-fit"]}
     [:div {:class ["card-body"]}
      [:h2 {:class ["card-title" "text-lg" "mb-4"]} "游戏控制"]

      ;; Game status
      [:div {:class ["alert" "alert-info" "mb-4"]}
       [:svg {:class ["w-6" "h-6"] :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
        [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                :d "M13 16h-1v-4h-1m1-4h.01M21 12a9 9 0 11-18 0 9 9 0 0118 0z"}]]
       [:span (str "轮到 " next-player " 方下棋")]]

      ;; Control buttons
      [:div {:class ["space-y-3"]}
       [:button {:class ["btn" "btn-secondary" "btn-block"]
                 :on {:click [[:debug/print "悔棋"]
                              [:xiangqi/go-back]]}}
        [:svg {:class ["w-5" "h-5"] :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M3 10h10a8 8 0 018 8v2M3 10l6 6m-6-6l6-6"}]]
        "悔棋"]

       [:button {:class ["btn" "btn-accent" "btn-block"]
                 :on {:click [[:debug/print "重新开始"]
                              [:xiangqi/restart]]}}
        [:svg {:class ["w-5" "h-5"] :fill "none" :stroke "currentColor" :viewBox "0 0 24 24"}
         [:path {:stroke-linecap "round" :stroke-linejoin "round" :stroke-width "2"
                 :d "M4 4v5h.582m15.356 2A8.001 8.001 0 004.582 9m0 0H9m11 11v-5h-.581m0 0a8.003 8.003 0 01-15.357-2m15.357 2H15"}]]
        "重新开始"]]

      ;; Game info
      [:div {:class ["divider"]}]
      [:div {:class ["text-sm" "text-base-content/70"]}
       [:p "点击棋子选择，再点击目标位置移动"]
       [:p "红色棋子为红方，黑色棋子为黑方"]]]]))

(defn main [state]
  [:div {:class ["min-h-screen" "bg-base-200" "p-4"]}
   ;; Header
   [:div {:class ["navbar" "bg-base-100" "shadow-lg" "rounded-box" "mb-6"]}
    [:div {:class ["navbar-start"]}
     [:h1 {:class ["text-2xl" "font-bold" "text-primary"]} "中国象棋"]]
    [:div {:class ["navbar-end"]}
     [:div {:class ["dropdown" "dropdown-end"]}
      [:label {:tabindex "0" :class ["btn" "btn-ghost" "btn-circle"]}
       [:svg {:class ["w-5" "h-5"] :fill "currentColor" :viewBox "0 0 20 20"}
        [:path {:d "M10 6a2 2 0 110-4 2 2 0 010 4zM10 12a2 2 0 110-4 2 2 0 010 4zM10 18a2 2 0 110-4 2 2 0 010 4z"}]]]
      [:ul {:tabindex "0" :class ["dropdown-content" "menu" "p-2" "shadow" "bg-base-100" "rounded-box" "w-52"]}
       [:li [:a {:on {:click [[:debug/print "导出棋谱"]
                              [:xiangqi/export-game-tree]]}} "导出棋谱"]]
       [:li [:a {:on {:click [[:debug/print "导入棋谱"]
                              [:xiangqi/import-game-tree]]}} "导入棋谱"]]]]]]
  
   ;; Main content area
   [:div {:class ["flex" "flex-col" "lg:flex-row" "gap-6"]}
    ;; Control panel
    (control-panel state)
  
    ;; Chessboard area
    [:div {:class ["flex-1" "flex" "justify-center"]}
     [:div {:class ["card" "bg-base-100" "shadow-xl"]}
      [:div {:class ["card-body" "p-6"]}
       (chessboard state)]]]]])
