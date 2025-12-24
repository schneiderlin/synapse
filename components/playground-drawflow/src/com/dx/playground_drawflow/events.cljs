(ns com.dx.playground-drawflow.events)

(def prefix :playground-drawflow/canvas)

;; Store handlers for cleanup
(defonce drag-handlers (atom {:move nil :up nil}))

(defn setup-drag-listeners
  "Set up document-level mouse listeners for dragging"
  [store]
  (let [handle-move (fn [e]
                      (let [current-state (get @store prefix {})
                            dragging (get current-state :dragging nil)]
                        (when (and dragging (= (:type dragging) :node))
                          (let [current-x (.-clientX e)
                                current-y (.-clientY e)
                                viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
                                zoom (:zoom viewport 1)
                                start-pos (:start-pos dragging)
                                node-start-pos (:node-start-pos dragging)
                                delta-x (/ (- current-x (:x start-pos)) zoom)
                                delta-y (/ (- current-y (:y start-pos)) zoom)
                                new-x (+ (:x node-start-pos) delta-x)
                                new-y (+ (:y node-start-pos) delta-y)]
                            (swap! store update-in [prefix :dragging :has-moved] (constantly true))
                            (swap! store update-in [prefix :nodes]
                                   (fn [nodes]
                                     (let [nodes-map (or nodes {})
                                           node-id (:node-id dragging)]
                                       (if-let [node (get nodes-map node-id)]
                                         (assoc nodes-map node-id (assoc-in node [:position] {:x new-x :y new-y}))
                                         nodes-map))))))))
        handle-up (fn [_e]
                    (let [current-state (get @store prefix {})
                          dragging (get current-state :dragging nil)]
                      (when (and dragging (= (:type dragging) :node))
                        (let [has-moved (:has-moved dragging false)
                              node-id (:node-id dragging)]
                          (swap! store assoc-in [prefix :dragging] nil)
                          (when (not has-moved)
                            ;; It was a click - select the node
                            (swap! store update-in [prefix :nodes]
                                   (fn [nodes]
                                     (let [nodes-map (or nodes {})]
                                       (reduce-kv (fn [acc n-id node]
                                                   (assoc acc n-id (assoc node :selected? (= n-id node-id))))
                                                 {}
                                                 nodes-map))))
                            (swap! store update-in [prefix :edges]
                                   (fn [edges]
                                     (mapv #(assoc % :selected? false) (or edges [])))))
                          ;; Remove listeners
                          (let [{:keys [move up]} @drag-handlers]
                            (when move
                              (.removeEventListener js/document "mousemove" move))
                            (when up
                              (.removeEventListener js/document "mouseup" up))
                            (reset! drag-handlers {:move nil :up nil}))))))]
    ;; Remove old listeners if any
    (let [{:keys [move up]} @drag-handlers]
      (when move
        (.removeEventListener js/document "mousemove" move))
      (when up
        (.removeEventListener js/document "mouseup" up)))
    ;; Store new handlers
    (reset! drag-handlers {:move handle-move :up handle-up})
    ;; Add new listeners
    (.addEventListener js/document "mousemove" handle-move)
    (.addEventListener js/document "mouseup" handle-up)))

;; TODO: Implement handle-edge-click in Phase 3
;; Handle click on an edge - will dispatch actions for selection
(defn handle-edge-click [_edge-id _event]
  nil)

;; Keyboard handlers for cleanup
(defonce keyboard-handlers (atom {:keydown nil}))

(defn setup-keyboard-listeners
  "Set up document-level keyboard listeners for delete operations"
  [store]
  (let [handle-keydown (fn [e]
                        (let [key (.-key e)
                              current-state (get @store prefix {})
                              edges (get current-state :edges [])
                              selected-edge-ids (set (map :id (filter :selected? edges)))]
                          (when (and (or (= key "Delete") (= key "Backspace"))
                                   (seq selected-edge-ids))
                            (.preventDefault e)
                            (swap! store update-in [prefix :edges]
                                   (fn [edges]
                                     (filterv #(not (contains? selected-edge-ids (:id %))) (or edges [])))))))]
    ;; Remove old listener if any
    (let [{:keys [keydown]} @keyboard-handlers]
      (when keydown
        (.removeEventListener js/document "keydown" keydown)))
    ;; Store new handler
    (reset! keyboard-handlers {:keydown handle-keydown})
    ;; Add new listener
    (.addEventListener js/document "keydown" handle-keydown)))

(defn cleanup-keyboard-listeners
  "Remove document-level keyboard listeners"
  []
  (let [{:keys [keydown]} @keyboard-handlers]
    (when keydown
      (.removeEventListener js/document "keydown" keydown)
      (reset! keyboard-handlers {:keydown nil}))))

;; Pan handlers for cleanup
(defonce pan-handlers (atom {:move nil :up nil}))

(defn setup-pan-listeners
  "Set up document-level mouse listeners for panning"
  [store]
  (let [handle-move (fn [e]
                      (let [current-state (get @store prefix {})
                            dragging (get current-state :dragging nil)]
                        (when (and dragging (= (:type dragging) :pan))
                          (let [current-x (.-clientX e)
                                current-y (.-clientY e)
                                start-pos (:start-pos dragging)
                                viewport-start (:viewport-start dragging)
                                delta-x (- current-x (:x start-pos))
                                delta-y (- current-y (:y start-pos))
                                new-x (+ (:x viewport-start) delta-x)
                                new-y (+ (:y viewport-start) delta-y)
                                viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
                            (swap! store assoc-in [prefix :viewport]
                                   {:x new-x :y new-y :zoom (:zoom viewport 1)})))))
        handle-up (fn [_e]
                    (let [current-state (get @store prefix {})
                          dragging (get current-state :dragging nil)]
                      (when (and dragging (= (:type dragging) :pan))
                        (swap! store assoc-in [prefix :dragging] nil)
                        ;; Remove listeners
                        (let [{:keys [move up]} @pan-handlers]
                          (when move
                            (.removeEventListener js/document "mousemove" move))
                          (when up
                            (.removeEventListener js/document "mouseup" up))
                          (reset! pan-handlers {:move nil :up nil})))))]
    ;; Remove old listeners if any
    (let [{:keys [move up]} @pan-handlers]
      (when move
        (.removeEventListener js/document "mousemove" move))
      (when up
        (.removeEventListener js/document "mouseup" up)))
    ;; Store new handlers
    (reset! pan-handlers {:move handle-move :up handle-up})
    ;; Add new listeners
    (.addEventListener js/document "mousemove" handle-move)
    (.addEventListener js/document "mouseup" handle-up)))
