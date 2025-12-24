(ns com.dx.playground-odoyle-rules.core
  (:require 
   [odoyle.rules :as o]))

;; 7 * 7 board
;; pirate: ship, flagship
;; kraken: kraken
;; corners are special tiles

(def init-board
  [[:corner :nil :kraken :nil :kraken :nil :corner]
   [:nil :nil :nil :nil :nil :nil :nil]
   [:kraken :nil :nil :ship :nil :nil :kraken]
   [:nil :nil :ship :flagship :ship :nil :nil]
   [:kraken :nil :nil :ship :nil :nil :kraken]
   [:nil :nil :nil :nil :nil :nil :nil]
   [:corner :nil :kraken :nil :kraken :nil :corner]])

;; Helper functions
(defn get-piece [board row col]
  (when (and (>= row 0) (< row (count board))
             (>= col 0) (< col (count (first board))))
    (get-in board [row col])))

(defn set-piece [board row col piece]
  (assoc-in board [row col] piece))

(defn valid-position? [row col]
  (and (>= row 0) (< row 7)
       (>= col 0) (< col 7)))

(defn player-piece? [piece player]
  (case player
    :kraken (= piece :kraken)
    :pirate (or (= piece :ship) (= piece :flagship))
    false))

(defn opponent-piece? [piece player]
  (case player
    :kraken (or (= piece :ship) (= piece :flagship))
    :pirate (= piece :kraken)
    false))

(defn get-player-pieces [board player]
  (for [[row-idx row] (map-indexed vector board)
        [col-idx cell] (map-indexed vector row)
        :when (player-piece? cell player)]
    [row-idx col-idx]))

(defn possible-moves [board row col]
  (let [directions [[-1 0] [1 0] [0 -1] [0 1]]]
    (->> directions
         (map (fn [[dr dc]] [(+ row dr) (+ col dc)]))
         (filter (fn [[r c]]
                   (and (valid-position? r c)
                        (let [piece (get-piece board r c)]
                          (or (nil? piece) (= piece :nil)))))))))

(defn check-capture [board row col player]
  "Check if piece at [row col] should be captured (XOX pattern)"
  (let [piece (get-piece board row col)]
    (when (opponent-piece? piece player)
      (or
       ;; Horizontal capture: XOX
       (and (valid-position? row (dec col))
            (valid-position? row (inc col))
            (player-piece? (get-piece board row (dec col)) player)
            (player-piece? (get-piece board row (inc col)) player))
       ;; Vertical capture: XOX
       (and (valid-position? (dec row) col)
            (valid-position? (inc row) col)
            (player-piece? (get-piece board (dec row) col) player)
            (player-piece? (get-piece board (inc row) col) player))))))

(defn apply-captures [board player]
  "Apply all captures for the current player"
  (reduce (fn [board [row col]]
            (if (check-capture board row col player)
              (set-piece board row col :nil)
              board))
          board
          (for [row (range 7)
                col (range 7)]
            [row col])))

(defn move-piece [board from-row from-col to-row to-col]
  "Move piece from [from-row from-col] to [to-row to-col]"
  (let [piece (get-piece board from-row from-col)]
    (-> board
        (set-piece to-row to-col piece)
        (set-piece from-row from-col :nil))))

(def rules
  (o/ruleset
   {::board-state
    [:what
     [::board ::current board]]

    ::current-turn
    [:what
     [::turn ::current current-player]]

    ::selected-piece
    [:what
     [::selected ::row row]
     [::selected ::col col]]

    ::calculate-possible-moves
    [:what
     [::board ::current board]
     [::selected ::row row]
     [::selected ::col col]
     [::turn ::current current-player]
     :when
     (player-piece? (get-piece board row col) current-player)
     :then
     (-> session
         (o/insert ::possible-moves ::positions (possible-moves board row col))
         o/reset!)]

    ::possible-moves
    [:what
     [::possible-moves ::positions positions]]

    ::execute-move
    [:what
     [::board ::current board]
     [::selected ::row from-row]
     [::selected ::col from-col]
     [::move ::to-row to-row]
     [::move ::to-col to-col]
     [::turn ::current current-player]
     :when
     (and (player-piece? (get-piece board from-row from-col) current-player)
          (some #(= % [to-row to-col]) (possible-moves board from-row from-col)))
     :then
     (let [new-board (-> board
                         (move-piece from-row from-col to-row to-col)
                         (apply-captures current-player))
           next-player (if (= current-player :kraken) :pirate :kraken)]
       (-> session
           (o/insert ::board ::current new-board)
           (o/insert ::turn ::current next-player)
           (o/retract ::selected ::row)
           (o/retract ::selected ::col)
           (o/retract ::move ::to-row)
           (o/retract ::move ::to-col)
           (o/retract ::possible-moves ::positions)
           o/reset!))]}))

;; create session and add rules
(def *session
  (atom (-> (reduce o/add-rule (o/->session) rules) 
            (o/insert ::board ::current init-board)
            (o/insert ::turn ::current :kraken)
            o/fire-rules)))

;; Game API functions
(defn select-piece [row col]
  "Select a piece at the given position"
  (swap! *session
         (fn [session]
           (-> session
               (o/insert ::selected ::row row)
               (o/insert ::selected ::col col)
               o/fire-rules))))

(defn move-selected-piece [to-row to-col]
  "Move the selected piece to the given position"
  (swap! *session
         (fn [session]
           (-> session
               (o/insert ::move ::to-row to-row)
               (o/insert ::move ::to-col to-col)
               o/fire-rules))))

(defn get-board []
  "Get the current board state"
  (->> (o/query-all @*session ::board-state)
       first
       :board))

(defn get-current-turn []
  "Get the current player's turn"
  (->> (o/query-all @*session ::current-turn)
       first
       :current-player))

(defn get-possible-moves []
  "Get possible moves for the selected piece"
  (or (->> (o/query-all @*session ::possible-moves)
           first
           :positions)
      []))

(defn reset-game []
  "Reset the game to initial state"
  (reset! *session
          (-> (reduce o/add-rule (o/->session) rules) 
              (o/insert ::board ::current init-board)
              (o/insert ::turn ::current :kraken)
              o/fire-rules)))

(comment
  ;; Example game play
  (reset-game)
  
  ;; Check initial state
  (= (get-board) 
     init-board)
  (get-current-turn)
  
  ;; Select a kraken piece
  (select-piece 0 2)
  (get-possible-moves) ; => possible moves for that piece
  
  ;; Move the selected piece
  (move-selected-piece 0 3)
  
  ;; Check new state
  (get-board)
  (get-current-turn)
  
  ;; Select a pirate piece
  (select-piece 3 2)
  (get-possible-moves)
  
  ;; Move pirate piece
  (move-selected-piece 3 1)
  
  ;; Check for captures
  (get-board)
  (get-current-turn) ; => :kraken
  
  :rcf)

