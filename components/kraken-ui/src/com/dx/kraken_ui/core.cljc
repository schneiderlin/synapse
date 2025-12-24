(ns com.zihao.kraken-ui.core
  (:require [com.zihao.kraken-ui.common :refer [prefix]]
            [com.zihao.playground-odoyle-rules.interface :as kraken]))

(defn piece->text [piece]
  (case piece
    :corner "⚫"
    :kraken "🐙"
    :ship "⛵"
    :flagship "🚢"
    :nil " "
    ""))

(defn- is-possible-move? [possible-moves row-idx col-idx]
  (some #(= % [row-idx col-idx]) possible-moves))

(defn render-cell [row-idx col-idx cell possible-moves] 
  (let [is-possible-move? (is-possible-move? possible-moves row-idx col-idx)]
    [:div {:key (str row-idx "-" col-idx)
           :class (cond-> ["w-12" "h-12" "flex" "items-center" "justify-center"
                           "bg-white" "border" "border-gray-300" "rounded"
                           "text-2xl" "font-bold"]
                    is-possible-move? (conj "bg-green-200" "border-green-500" "border-2"))
           :on {:click (if is-possible-move?
                        [[:kraken/move-selected-piece row-idx col-idx]]
                        [[:kraken/select-piece row-idx col-idx]])}}
     (piece->text cell)]))

(defn render-board [state]
  (let [{:keys [board possible-moves]
         :or {board kraken/init-board
              possible-moves []}} (prefix state)]
    [:div {:class ["grid" "grid-cols-7" "gap-1" "p-4" "bg-indigo-400"
                   "rounded-lg" "max-w-fit"]}
     (mapcat (fn [[row-idx row]]
               (map (fn [[col-idx cell]]
                      (render-cell row-idx col-idx cell possible-moves))
                    (map-indexed vector row)))
             (map-indexed vector board))]))


