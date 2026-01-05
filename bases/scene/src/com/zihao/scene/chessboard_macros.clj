(ns com.zihao.scene.chessboard-macros)

(defmacro defscene-from-fen [scene-name fen-string]
  `(portfolio.replicant/defscene ~scene-name
     :params (atom (com.zihao.scene.chessboard/xiangqi-state->state (~'fen/fen->state ~fen-string)))
     :on-mount (fn [~'store]
                 (~'r/set-dispatch!
                  (fn [{:keys [replicant/dom-event]} ~'actions]
                    (let [~'execute-fn (apply ~'make-execute-f [~'xiangqi-actions/execute-action])]
                      (->> ~'actions
                           (~'interpolate ~'dom-event)
                           (~'execute-fn {:store ~'store} ~'dom-event))))))
     [~'store]
     (~'render/chessboard @~'store)))

