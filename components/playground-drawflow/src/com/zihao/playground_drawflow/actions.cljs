(ns com.zihao.playground-drawflow.actions
  (:require
   [com.zihao.playground-drawflow.events :as events]
   [com.zihao.playground-drawflow.nodes :as nodes]
   [com.zihao.playground-drawflow.viewport :as viewport]
   [com.zihao.playground-drawflow.edges :as edges]))

(def prefix :playground-drawflow/canvas)

;; Node actions
(defn handle-add-node [_store _event args]
  (let [[node-data] args
        node-id (or (:id node-data) (str "node-" (random-uuid)))
        new-node (merge {:id node-id
                         :type "default"
                         :position {:x 100 :y 100}
                         :data {}
                         :selected? false}
                        node-data)]
    [[:store/update-in [prefix :nodes] (fn [nodes] (assoc (or nodes {}) node-id new-node))]
     ;; Trigger measurement after node is added (measurement will happen after render)
     [:canvas/measure-handle-offsets]]))

(defn handle-update-node [_store _event args]
  (let [[node-id updates] args]
    [[:store/update-in [prefix :nodes]
      (fn [nodes]
        (if-let [node (get (or nodes {}) node-id)]
          (assoc nodes node-id (merge node updates))
          nodes))]]))

(defn handle-delete-node [store _event args]
  (let [[node-id] args
        current-state (get @store prefix {})
        edges (get current-state :edges [])
        nodes-map (get current-state :nodes {})
        ;; Find edges connected to this node (as source or target)
        connected-edges (filterv #(or (= (:source %) node-id)
                                      (= (:target %) node-id))
                                 edges)
        ;; Find target nodes that need to be updated (edges where this node is the source)
        target-nodes-to-update (reduce (fn [acc edge]
                                         (if (= (:source edge) node-id)
                                           (let [target-node-id (:target edge)
                                                 target-handle-id (:targetHandle edge)]
                                             (if (and target-node-id target-handle-id)
                                               (conj acc {:node-id target-node-id
                                                          :handle-id target-handle-id})
                                               acc))
                                           acc))
                                       []
                                       connected-edges)]
    (if (seq target-nodes-to-update)
      ;; Update target handles and delete edges
      (let [node-updates (reduce (fn [acc {:keys [node-id handle-id]}]
                                  (let [target-node (get nodes-map node-id)]
                                    (if target-node
                                      (let [handle-data-map (get-in target-node [:data :handle-data] {})
                                            handle-data (get handle-data-map handle-id {})
                                            preserved-direct-value (:direct-value handle-data)
                                            updated-handle-data (assoc handle-data
                                                                     :input-mode :direct
                                                                     :connection nil
                                                                     :direct-value preserved-direct-value)
                                            updated-handle-data-map (assoc handle-data-map handle-id updated-handle-data)
                                            updated-node-data (assoc-in (or (:data target-node) {}) [:handle-data] updated-handle-data-map)
                                            updated-node (assoc target-node :data updated-node-data)]
                                        (assoc acc node-id updated-node))
                                      acc)))
                                {}
                                target-nodes-to-update)]
        [[:store/update-in [prefix :nodes]
          (fn [nodes]
            (reduce-kv (fn [acc n-id updated-node]
                        (assoc acc n-id updated-node))
                      (dissoc (or nodes {}) node-id)
                      node-updates))]
         [:store/update-in [prefix :edges]
          (fn [edges]
            (filterv #(and (not= (:source %) node-id)
                         (not= (:target %) node-id))
                   (or edges [])))]
         ;; Clear handle offsets for deleted node and trigger re-measurement
         [:store/update-in [prefix :handle-offsets] (fn [offsets] (dissoc (or offsets {}) node-id))]
         [:canvas/measure-handle-offsets]])
      ;; No target nodes to update - just delete node and edges
      [[:store/update-in [prefix :nodes]
        (fn [nodes]
          (dissoc (or nodes {}) node-id))]
       [:store/update-in [prefix :edges]
        (fn [edges]
          (filterv #(and (not= (:source %) node-id)
                       (not= (:target %) node-id))
                 (or edges [])))]
       ;; Clear handle offsets for deleted node and trigger re-measurement
       [:store/update-in [prefix :handle-offsets] (fn [offsets] (dissoc (or offsets {}) node-id))]
       [:canvas/measure-handle-offsets]])))

;; Edge actions
(defn handle-add-edge [store _event args]
  (let [[edge-data] args
        edge-id (or (:id edge-data) (str "edge-" (random-uuid)))
        new-edge (merge {:id edge-id
                         :type "bezier"
                         :selected? false}
                        edge-data)
        target-node-id (:target new-edge)
        target-handle-id (:targetHandle new-edge)
        source-node-id (:source new-edge)
        source-handle-id (:sourceHandle new-edge)
        current-state (get @store prefix {})
        nodes-map (get current-state :nodes {})
        edges (get current-state :edges [])
        ;; Find existing edge to the same target handle (if any)
        existing-edge (first (filter (fn [edge]
                                       (and (= (:target edge) target-node-id)
                                            (= (:targetHandle edge) target-handle-id)))
                                     edges))
        existing-edge-id (when existing-edge (:id existing-edge))]
    (if (and target-node-id target-handle-id)
      ;; Update target handle to connected mode
      (let [target-node (get nodes-map target-node-id)]
        (if target-node
          (let [handle-data-map (get-in target-node [:data :handle-data] {})
                handle-data (get handle-data-map target-handle-id {})
                ;; Preserve direct value when switching to connected mode
                preserved-direct-value (:direct-value handle-data)
                updated-handle-data (assoc handle-data
                                          :input-mode :connected
                                          :connection {:source-node-id source-node-id
                                                      :source-handle-id source-handle-id}
                                          ;; Preserve direct value (hidden but available)
                                          :direct-value preserved-direct-value)
                updated-handle-data-map (assoc handle-data-map target-handle-id updated-handle-data)
                updated-node-data (assoc-in (or (:data target-node) {}) [:handle-data] updated-handle-data-map)
                updated-node (assoc target-node :data updated-node-data)]
            (if existing-edge-id
              ;; Replace existing edge
              [[:store/update-in [prefix :edges]
                (fn [edges]
                  (->> edges
                       (filterv #(not= (:id %) existing-edge-id))
                       (conj new-edge)))]
               [:store/update-in [prefix :nodes]
                (fn [nodes] (assoc (or nodes {}) target-node-id updated-node))]]
              ;; Add new edge
              [[:store/update-in [prefix :edges] (fn [edges] (conj (or edges []) new-edge))]
               [:store/update-in [prefix :nodes] (fn [nodes] (assoc (or nodes {}) target-node-id updated-node))]]))
          ;; Fallback if node not found
          (if existing-edge-id
            [[:store/update-in [prefix :edges]
              (fn [edges]
                (->> edges
                     (filterv #(not= (:id %) existing-edge-id))
                     (conj new-edge)))]]
            [[:store/update-in [prefix :edges] (fn [edges] (conj (or edges []) new-edge))]])))
      ;; No target handle - just add edge (or replace if exists)
      (if existing-edge-id
        [[:store/update-in [prefix :edges]
          (fn [edges]
            (->> edges
                 (filterv #(not= (:id %) existing-edge-id))
                 (conj new-edge)))]]
        [[:store/update-in [prefix :edges] (fn [edges] (conj (or edges []) new-edge))]]))))

(defn handle-delete-edge [store _event args]
  (let [[edge-id] args
        current-state (get @store prefix {})
        edges (get current-state :edges [])
        edge-to-delete (first (filter #(= (:id %) edge-id) edges))
        target-node-id (:target edge-to-delete)
        target-handle-id (:targetHandle edge-to-delete)]
    (if (and target-node-id target-handle-id)
      ;; Update target handle to direct mode (preserve direct value if exists)
      (let [nodes-map (get current-state :nodes {})
            target-node (get nodes-map target-node-id)]
        (when target-node
          (let [handle-data-map (get-in target-node [:data :handle-data] {})
                handle-data (get handle-data-map target-handle-id {})
                ;; Preserve direct value, switch to direct mode
                preserved-direct-value (:direct-value handle-data)
                updated-handle-data (assoc handle-data
                                          :input-mode :direct
                                          :connection nil
                                          :direct-value preserved-direct-value)
                updated-handle-data-map (assoc handle-data-map target-handle-id updated-handle-data)
                updated-node-data (assoc-in (or (:data target-node) {}) [:handle-data] updated-handle-data-map)
                updated-node (assoc target-node :data updated-node-data)]
            [[:store/update-in [prefix :edges]
              (fn [edges]
                (filterv #(not= (:id %) edge-id) (or edges [])))]
             [:store/update-in [prefix :nodes]
              (fn [nodes]
                (assoc (or nodes {}) target-node-id updated-node))]])))
      ;; No target handle - just delete edge
      [[:store/update-in [prefix :edges]
        (fn [edges]
          (filterv #(not= (:id %) edge-id) (or edges [])))]])))

;; Selection actions
(defn handle-select-node [_store _event item-id]
  [[:store/update-in [prefix :nodes]
    (fn [nodes]
      (let [nodes-map (or nodes {})]
        (reduce-kv (fn [acc node-id node]
                     (assoc acc node-id (assoc node :selected? (= node-id item-id))))
                   {}
                   nodes-map)))]
   [:store/update-in [prefix :edges]
    (fn [edges]
      (mapv #(assoc % :selected? false) (or edges [])))]])

(defn handle-select-edge [_store _event item-id]
  [[:store/update-in [prefix :edges]
    (fn [edges]
      (mapv (fn [edge]
              (assoc edge :selected? (= (:id edge) item-id)))
            (or edges [])))]
   [:store/update-in [prefix :nodes]
    (fn [nodes]
      (let [nodes-map (or nodes {})]
        (reduce-kv (fn [acc node-id node]
                     (assoc acc node-id (assoc node :selected? false)))
                   {}
                   nodes-map)))]])

(defn handle-clear-selection [_store _event]
  [[:store/update-in [prefix :nodes]
    (fn [nodes]
      (let [nodes-map (or nodes {})]
        (reduce-kv (fn [acc node-id node]
                     (assoc acc node-id (assoc node :selected? false)))
                   {}
                   nodes-map)))]
   [:store/update-in [prefix :edges]
    (fn [edges]
      (mapv #(assoc % :selected? false) (or edges [])))]])

(defn handle-select [store event args]
  (let [[selection-type item-id] args]
    (case selection-type
      :node (handle-select-node store event item-id)
      :edge (handle-select-edge store event item-id)
      :clear (handle-clear-selection store event)
      nil)))

;; Viewport actions
(defn handle-pan [store _event args]
  (let [[delta-x delta-y] args
        current-state (get @store prefix {})
        current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
    [[:store/assoc-in [prefix :viewport]
      (-> current-viewport
          (update :x (fn [x] (+ (or x 0) delta-x)))
          (update :y (fn [y] (+ (or y 0) delta-y))))]]))

(defn handle-canvas-mouse-down
  "Handle mouse down on canvas background (not on nodes/edges) - start pan"
  [store _event args]
  (let [[event] args]
    (when event
      (let [target (.-target event)
            ;; Check if target is a node, handle, or edge
            ;; These should stop propagation, but we check anyway to be safe
            is-node-or-edge (let [class-list (when (.-classList target) (.-classList target))
                                  id (.-id target)
                                  tag-name (.-tagName target)]
                              (or (and class-list (or (.contains class-list "node")
                                                     (.contains class-list "handle")
                                                     (.contains class-list "edge")))
                                  (and id (or (and (not= id "") (.startsWith id "node-"))
                                            (and (not= id "") (.startsWith id "handle-"))
                                            (and (not= id "") (.startsWith id "edge-"))))
                                  ;; SVG paths for edges
                                  (and (= tag-name "path") 
                                       class-list 
                                       (.contains class-list "edge"))))
            ;; If it's not a node/edge/handle, and the event reached here (meaning nodes/edges didn't stop it),
            ;; then it's a background click
            is-background (not is-node-or-edge)
            start-x (.-clientX event)
            start-y (.-clientY event)
            current-state (get @store prefix {})
            current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
        (when is-background
          ;; Set up document-level listeners for pan
          (events/setup-pan-listeners store)
          [[:store/assoc-in [prefix :dragging]
            {:type :pan
             :start-pos {:x start-x :y start-y}
             :viewport-start {:x (:x current-viewport) :y (:y current-viewport)}}]
           [:event/prevent-default]
           [:event/stop-propagation]])))))

(defn handle-zoom [store _event args]
  (let [[zoom-delta zoom-center-x zoom-center-y] args
        current-state (get @store prefix {})
        current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
        current-zoom (:zoom current-viewport 1)
        new-zoom (max 0.1 (min 2.0 (+ current-zoom zoom-delta)))]
    ;; Zoom around a point (mouse position)
    (if (and zoom-center-x zoom-center-y)
      (let [zoom-ratio (/ new-zoom current-zoom)
            current-x (:x current-viewport 0)
            current-y (:y current-viewport 0)
            new-x (- zoom-center-x (* (- zoom-center-x current-x) zoom-ratio))
            new-y (- zoom-center-y (* (- zoom-center-y current-y) zoom-ratio))]
        [[:store/assoc-in [prefix :viewport]
          {:x new-x :y new-y :zoom new-zoom}]])
      [[:store/assoc-in [prefix :viewport :zoom] new-zoom]])))

(defn handle-canvas-wheel
  "Handle mouse wheel on canvas - zoom in/out around cursor position"
  [store _event args]
  (let [[event] args]
    (when event
      (let [delta-y (.-deltaY event)
            ;; Prevent default scrolling
            _ (.preventDefault event)
            ;; Get mouse position relative to canvas container
            container-elem (.querySelector js/document ".canvas-container")
            container-rect (when container-elem (.getBoundingClientRect container-elem))
            mouse-x (.-clientX event)
            mouse-y (.-clientY event)
            current-state (get @store prefix {})
            current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
            current-zoom (:zoom current-viewport 1)
            ;; Calculate zoom delta (negative delta-y = zoom in, positive = zoom out)
            ;; Use exponential scaling for smoother zoom
            zoom-factor (if (< delta-y 0) 1.1 0.9)
            new-zoom (max 0.1 (min 2.0 (* current-zoom zoom-factor)))]
        (when container-rect
          ;; Convert mouse position to canvas coordinates (before zoom)
          (let [canvas-pos (viewport/screen->canvas mouse-x mouse-y current-viewport container-rect)
                ;; Mouse position in screen coordinates relative to container
                screen-x (- mouse-x (.-left container-rect))
                screen-y (- mouse-y (.-top container-rect))
                ;; Calculate new viewport offset to keep canvas point under cursor
                ;; Formula: new_viewport_x = screen_x - canvas_x * new_zoom
                new-x (- screen-x (* (:x canvas-pos) new-zoom))
                new-y (- screen-y (* (:y canvas-pos) new-zoom))]
            [[:store/assoc-in [prefix :viewport]
              {:x new-x :y new-y :zoom new-zoom}]
             [:event/prevent-default]
             [:event/stop-propagation]]))))))

(defn handle-canvas-touch-start
  "Handle touch start - track touches for pinch gesture"
  [store _event args]
  (let [[event] args]
    (when event
      (let [touches (.-touches event)
            touch-count (.-length touches)]
        (when (>= touch-count 2)
          ;; Two-finger touch - start pinch gesture
          (let [touch1 (aget touches 0)
                touch2 (aget touches 1)
                x1 (.-clientX touch1)
                y1 (.-clientY touch1)
                x2 (.-clientX touch2)
                y2 (.-clientY touch2)
                ;; Calculate initial distance
                initial-distance (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))
                ;; Calculate midpoint
                mid-x (/ (+ x1 x2) 2)
                mid-y (/ (+ y1 y2) 2)
                current-state (get @store prefix {})
                current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
            [[:store/assoc-in [prefix :dragging]
              {:type :pinch
               :initial-distance initial-distance
               :initial-zoom (:zoom current-viewport 1)
               :center-pos {:x mid-x :y mid-y}
               :viewport-start {:x (:x current-viewport) :y (:y current-viewport)}}]
             [:event/prevent-default]
             [:event/stop-propagation]]))))))

(defn handle-canvas-touch-move
  "Handle touch move - update pinch zoom"
  [store _event args]
  (let [[event] args]
    (when event
      (let [touches (.-touches event)
            touch-count (.-length touches)
            current-state (get @store prefix {})
            dragging (get current-state :dragging nil)]
        (when (and (>= touch-count 2) dragging (= (:type dragging) :pinch))
          (let [touch1 (aget touches 0)
                touch2 (aget touches 1)
                x1 (.-clientX touch1)
                y1 (.-clientY touch1)
                x2 (.-clientX touch2)
                y2 (.-clientY touch2)
                ;; Calculate current distance
                current-distance (Math/sqrt (+ (Math/pow (- x2 x1) 2) (Math/pow (- y2 y1) 2)))
                ;; Calculate zoom ratio
                distance-ratio (/ current-distance (:initial-distance dragging))
                new-zoom (max 0.1 (min 2.0 (* (:initial-zoom dragging) distance-ratio)))
                ;; Get midpoint
                mid-x (/ (+ x1 x2) 2)
                mid-y (/ (+ y1 y2) 2)
                container-elem (.querySelector js/document ".canvas-container")
                container-rect (when container-elem (.getBoundingClientRect container-elem))]
            (when container-rect
              (let [current-viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
                    ;; Convert midpoint to canvas coordinates (before zoom)
                    canvas-pos (viewport/screen->canvas mid-x mid-y current-viewport container-rect)
                    ;; Midpoint in screen coordinates relative to container
                    screen-x (- mid-x (.-left container-rect))
                    screen-y (- mid-y (.-top container-rect))
                    ;; Calculate new viewport offset to keep canvas point under midpoint
                    new-x (- screen-x (* (:x canvas-pos) new-zoom))
                    new-y (- screen-y (* (:y canvas-pos) new-zoom))]
                [[:store/assoc-in [prefix :viewport]
                  {:x new-x :y new-y :zoom new-zoom}]
                 [:event/prevent-default]
                 [:event/stop-propagation]]))))))))

(defn handle-canvas-touch-end
  "Handle touch end - end pinch gesture"
  [store _event args]
  (let [[event] args]
    (when event
      (let [current-state (get @store prefix {})
            dragging (get current-state :dragging nil)]
        (when (and dragging (= (:type dragging) :pinch))
          [[:store/assoc-in [prefix :dragging] nil]
           [:event/prevent-default]
           [:event/stop-propagation]])))))

(defn handle-set-viewport [_store _event args]
  (let [[viewport-data] args]
    [[:store/assoc-in [prefix :viewport] viewport-data]]))

;; Dragging actions
(defn handle-node-mouse-down [store _event args]
  (let [[node-id event] args
        current-state (get @store prefix {})
        nodes (get current-state :nodes {})
        node (get nodes node-id)
        node-pos (:position node)]
    (when (and node event)
      (let [start-x (.-clientX event)
            start-y (.-clientY event)]
        ;; Set up document-level listeners
        (events/setup-drag-listeners store)
        ;; Set up keyboard listeners if not already set up
        (events/setup-keyboard-listeners store)
        [[:canvas/select :node node-id]
         [:store/assoc-in [prefix :dragging]
          {:type :node
           :node-id node-id
           :start-pos {:x start-x :y start-y}
           :node-start-pos node-pos
           :has-moved false}]
         [:event/prevent-default]
         [:event/stop-propagation]]))))

(defn handle-mouse-move [store _event args]
  (let [[event] args
        current-state (get @store prefix {})
        dragging (get current-state :dragging nil)
        connection (get current-state :connection nil)
        tooltip-state (get current-state :tooltip nil)
        viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
    (cond
      ;; Handle node dragging
      (and event dragging (= (:type dragging) :node))
      (let [current-x (.-clientX event)
            current-y (.-clientY event)
            zoom (:zoom viewport 1)
            start-pos (:start-pos dragging)
            node-start-pos (:node-start-pos dragging)
            delta-x (/ (- current-x (:x start-pos)) zoom)
            delta-y (/ (- current-y (:y start-pos)) zoom)
            new-x (+ (:x node-start-pos) delta-x)
            new-y (+ (:y node-start-pos) delta-y)]
        [[:store/update-in [prefix :dragging :has-moved] (constantly true)]
         [:store/update-in [prefix :nodes]
          (fn [nodes]
            (let [nodes-map (or nodes {})
                  node-id (:node-id dragging)]
              (if-let [node (get nodes-map node-id)]
                (assoc nodes-map node-id (assoc-in node [:position] {:x new-x :y new-y}))
                nodes-map)))]])
      
      ;; Handle pan dragging
      (and event dragging (= (:type dragging) :pan))
      (let [current-x (.-clientX event)
            current-y (.-clientY event)
            start-pos (:start-pos dragging)
            viewport-start (:viewport-start dragging)
            delta-x (- current-x (:x start-pos))
            delta-y (- current-y (:y start-pos))
            new-x (+ (:x viewport-start) delta-x)
            new-y (+ (:y viewport-start) delta-y)]
        [[:store/assoc-in [prefix :viewport]
          {:x new-x :y new-y :zoom (:zoom viewport 1)}]])
      
      ;; Handle connection position update
      (and event connection (= (:type connection) :edge))
      (let [current-x (.-clientX event)
            current-y (.-clientY event)]
        [[:canvas/update-connection-position current-x current-y]])
      
      ;; Handle tooltip position update (when tooltip is visible and not hovering over tooltip itself)
      (and event tooltip-state (not (:hovering-tooltip? tooltip-state)))
      [[:canvas/update-tooltip-position event]]
      
      :else nil)))

(defn handle-mouse-up [store _event args]
  (let [[event] args
        current-state (get @store prefix {})
        dragging (get current-state :dragging nil)
        connection (get current-state :connection nil)]
    (cond
      ;; Handle node drag end
      (and event dragging (= (:type dragging) :node))
      (let [has-moved (:has-moved dragging false)
            node-id (:node-id dragging)]
        (if has-moved
          ;; It was a drag - just end it
          [[:store/assoc-in [prefix :dragging] nil]]
          ;; It was a click - select the node
          [[:store/assoc-in [prefix :dragging] nil]
           [:canvas/select :node node-id]]))
      
      ;; Handle pan drag end
      (and event dragging (= (:type dragging) :pan))
      [[:store/assoc-in [prefix :dragging] nil]]
      
      ;; Handle connection cancellation (mouse up on background)
      (and event connection (= (:type connection) :edge))
      [[:canvas/cancel-connection]]
      
      :else nil)))

(defn handle-end-drag [_store _event _args]
  [[:store/assoc-in [prefix :dragging] nil]])

;; Measurement actions
(defn handle-measure-handle-offsets
  "Trigger measurement of handle offsets after DOM is ready."
  [store _event _args]
  (js/requestAnimationFrame
   (fn []
     (let [current-state (get @store prefix {})
           nodes-map (get current-state :nodes {})
           measured-offsets (edges/measure-handle-offsets nodes-map)]
       (when (seq measured-offsets)
         (swap! store assoc-in [prefix :handle-offsets] measured-offsets)))))
  ;; Return empty actions since we're using requestAnimationFrame
  [])

;; Connection actions
(defn handle-handle-mouse-down [store _event args]
  (let [[node-id handle-type handle-id event] args
        current-state (get @store prefix {})
        nodes-map (get current-state :nodes {})
        handle-offsets (get current-state :handle-offsets {})
        node (get nodes-map node-id)]
    (when (and node event)
      (let [handle-pos (if handle-offsets
                        ;; Use measured offset if available
                        (edges/get-handle-position-in-canvas node handle-id handle-type handle-offsets)
                        ;; Fallback to calculated position
                        (nodes/get-handle-position node handle-type handle-id))]
        (if (= handle-type :source)
          ;; Start connection from source handle
          [[:store/assoc-in [prefix :connection]
            {:type :edge
             :source-node node-id
             :source-handle handle-id
             :position {:x (:x handle-pos) :y (:y handle-pos)}}]
           [:event/prevent-default]
           [:event/stop-propagation]]
          ;; Target handle - just prevent node drag, connection will complete on mouseup
          [[:event/prevent-default]
           [:event/stop-propagation]])))))

(defn handle-handle-mouse-up [store _event args]
  (let [[node-id handle-type handle-id event] args
        current-state (get @store prefix {})
        connection (get current-state :connection nil)]
    (when event
      (if (and connection (= (:type connection) :edge)
               (= handle-type :target) 
               (not= (:source-node connection) node-id))
        ;; Complete connection to target handle
        [[:canvas/complete-connection node-id handle-id]
         [:event/prevent-default]
         [:event/stop-propagation]]
        ;; If there's an active connection but this isn't a valid target, cancel it
        (if (and connection (= (:type connection) :edge))
          [[:canvas/cancel-connection]
           [:event/prevent-default]
           [:event/stop-propagation]]
          ;; No active connection - just prevent default
          [[:event/prevent-default]
           [:event/stop-propagation]])))))

(defn handle-complete-connection [store _event args]
  (let [[target-node-id target-handle-id] args
        current-state (get @store prefix {})
        connection (get current-state :connection nil)]
    (when (and connection (= (:type connection) :edge))
      (let [source-node-id (:source-node connection)
            source-handle-id (:source-handle connection)]
        (when (not= source-node-id target-node-id)
          ;; The handle-add-edge action will update the target handle's input-mode
          [[:canvas/add-edge {:source source-node-id
                              :target target-node-id
                              :sourceHandle source-handle-id
                              :targetHandle target-handle-id
                              :type "bezier"}]
           [:store/assoc-in [prefix :connection] nil]])))))

(defn handle-update-connection-position [store _event args]
  (let [[screen-x screen-y] args
        current-state (get @store prefix {})
        connection (get current-state :connection nil)
        viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})]
    (when (and connection (= (:type connection) :edge))
      (let [container-elem (.querySelector js/document ".canvas-container")
            container-rect (when container-elem (.getBoundingClientRect container-elem))
            canvas-pos (when container-rect
                         (viewport/screen->canvas screen-x screen-y viewport container-rect))]
        (when canvas-pos
          [[:store/assoc-in [prefix :connection :position] canvas-pos]])))))

(defn handle-cancel-connection [_store _event _args]
  [[:store/assoc-in [prefix :connection] nil]])

;; Edge selection
(defn handle-edge-click [store _event args]
  (let [[edge-id event] args]
    (when event
      ;; Set up keyboard listeners if not already set up
      (events/setup-keyboard-listeners store)
      [[:canvas/select :edge edge-id]
       [:event/prevent-default]
       [:event/stop-propagation]])))

(defn handle-delete-selected-edges [store _event _args]
  (let [current-state (get @store prefix {})
        edges (get current-state :edges [])
        selected-edges (filter :selected? edges)
        selected-edge-ids (set (map :id selected-edges))
        nodes-map (get current-state :nodes {})]
    (when (seq selected-edge-ids)
      ;; Update all target handles to direct mode
      (let [node-updates (reduce (fn [acc edge]
                                   (let [target-node-id (:target edge)
                                         target-handle-id (:targetHandle edge)]
                                     (if (and target-node-id target-handle-id)
                                       (let [target-node (get nodes-map target-node-id)]
                                         (if target-node
                                           (let [handle-data-map (get-in target-node [:data :handle-data] {})
                                                 handle-data (get handle-data-map target-handle-id {})
                                                 preserved-direct-value (:direct-value handle-data)
                                                 updated-handle-data (assoc handle-data
                                                                           :input-mode :direct
                                                                           :connection nil
                                                                           :direct-value preserved-direct-value)
                                                 updated-handle-data-map (assoc handle-data-map target-handle-id updated-handle-data)
                                                 updated-node-data (assoc-in (or (:data target-node) {}) [:handle-data] updated-handle-data-map)
                                                 updated-node (assoc target-node :data updated-node-data)]
                                             (assoc acc target-node-id updated-node))
                                           acc))
                                       acc)))
                                 {}
                                 selected-edges)]
        (if (seq node-updates)
          [[:store/update-in [prefix :edges]
            (fn [edges]
              (filterv #(not (contains? selected-edge-ids (:id %))) (or edges [])))]
           [:store/update-in [prefix :nodes]
            (fn [nodes]
              (reduce-kv (fn [acc node-id updated-node]
                          (assoc acc node-id updated-node))
                        (or nodes {})
                        node-updates))]]
          [[:store/update-in [prefix :edges]
            (fn [edges]
              (filterv #(not (contains? selected-edge-ids (:id %))) (or edges [])))]])))))

(defn handle-delete-selected-if-key
  "Handle Delete or Backspace key to delete selected edges"
  [_store event _args]
  (let [key (.-key event)]
    (when (or (= key "Delete") (= key "Backspace"))
      [[:canvas/delete-selected-edges]
       [:event/prevent-default]])))

;; Tooltip actions
(defn handle-handle-mouse-enter [store _event args]
  (let [[node-id handle-type handle-id event] args
        current-state (get @store prefix {})
        viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
        container-elem (.querySelector js/document ".canvas-container")
        container-rect (when container-elem (.getBoundingClientRect container-elem))
        nodes-map (get current-state :nodes {})
        node (get nodes-map node-id)]
    (when (and node event container-rect)
      ;; Cancel any existing tooltip timeout
      (let [existing-timeout (get-in current-state [:tooltip :timeout-id])]
        (when existing-timeout
          (js/clearTimeout existing-timeout)))
      ;; Calculate handle position in screen coordinates
      (let [handle-pos (nodes/get-handle-position node handle-type handle-id)
            canvas-x (:x handle-pos)
            canvas-y (:y handle-pos)
            screen-pos (viewport/canvas->screen canvas-x canvas-y viewport container-rect)
            ;; Set up delayed tooltip show
            timeout-id (js/setTimeout
                        (fn []
                          (let [updated-state (get @store prefix {})
                                updated-viewport (get updated-state :viewport {:x 0 :y 0 :zoom 1})
                                updated-container-elem (.querySelector js/document ".canvas-container")
                                updated-container-rect (when updated-container-elem (.getBoundingClientRect updated-container-elem))]
                            (when updated-container-rect
                              (let [updated-screen-pos (viewport/canvas->screen canvas-x canvas-y updated-viewport updated-container-rect)]
                                (swap! store assoc-in [prefix :tooltip]
                                       {:node-id node-id
                                        :handle-id handle-id
                                        :handle-type handle-type
                                        :position {:x (:x updated-screen-pos) :y (:y updated-screen-pos)}
                                        :timeout-id nil})))))
                        (get-in current-state [:tooltip :delay-ms] 300))]
        [[:store/assoc-in [prefix :tooltip]
          {:node-id node-id
           :handle-id handle-id
           :handle-type handle-type
           :position {:x (:x screen-pos) :y (:y screen-pos)}
           :timeout-id timeout-id}]]))))

(defn handle-handle-mouse-leave [store _event args]
  (let [[_node-id _handle-type _handle-id _event] args
        current-state (get @store prefix {})
        tooltip-state (get current-state :tooltip nil)]
    (when tooltip-state
      ;; Cancel timeout if still pending
      (let [timeout-id (:timeout-id tooltip-state)]
        (when timeout-id
          (js/clearTimeout timeout-id)))
      ;; Only hide if mouse is not moving to tooltip
      ;; We'll check this in a small delay to allow mouse to move to tooltip
      (let [hide-timeout-id (js/setTimeout
                            (fn []
                              (let [updated-state (get @store prefix {})
                                    updated-tooltip (get updated-state :tooltip nil)]
                                ;; Only hide if tooltip is still for this handle
                                (when (and updated-tooltip
                                         (= (:node-id updated-tooltip) (:node-id tooltip-state))
                                         (= (:handle-id updated-tooltip) (:handle-id tooltip-state))
                                         (not (:hovering-tooltip? updated-tooltip)))
                                  (swap! store assoc-in [prefix :tooltip] nil))))
                            50)]
        [[:store/assoc-in [prefix :tooltip :hide-timeout-id] hide-timeout-id]]))))

(defn handle-tooltip-mouse-enter [store _event _args]
  (let [current-state (get @store prefix {})
        tooltip-state (get current-state :tooltip nil)]
    (when tooltip-state
      ;; Cancel hide timeout
      (let [hide-timeout-id (:hide-timeout-id tooltip-state)]
        (when hide-timeout-id
          (js/clearTimeout hide-timeout-id)))
      [[:store/assoc-in [prefix :tooltip :hovering-tooltip?] true]])))

(defn handle-tooltip-mouse-leave [store _event _args]
  (let [current-state (get @store prefix {})
        tooltip-state (get current-state :tooltip nil)]
    (when tooltip-state
      ;; Hide tooltip after small delay
      (let [hide-timeout-id (js/setTimeout
                            (fn []
                              (swap! store assoc-in [prefix :tooltip] nil))
                            50)]
        [[:store/assoc-in [prefix :tooltip :hide-timeout-id] hide-timeout-id]
         [:store/assoc-in [prefix :tooltip :hovering-tooltip?] false]]))))

(defn handle-update-tooltip-position [store _event args]
  (let [[event] args
        current-state (get @store prefix {})
        tooltip-state (get current-state :tooltip nil)
        viewport (get current-state :viewport {:x 0 :y 0 :zoom 1})
        container-elem (.querySelector js/document ".canvas-container")
        container-rect (when container-elem (.getBoundingClientRect container-elem))]
    (when (and tooltip-state event container-rect)
      (let [nodes-map (get current-state :nodes {})
            node (get nodes-map (:node-id tooltip-state))
            handle-pos (when node
                        (nodes/get-handle-position node (:handle-type tooltip-state) (:handle-id tooltip-state)))]
        (when handle-pos
          (let [canvas-x (:x handle-pos)
                canvas-y (:y handle-pos)
                screen-pos (viewport/canvas->screen canvas-x canvas-y viewport container-rect)]
            [[:store/assoc-in [prefix :tooltip :position]
              {:x (:x screen-pos) :y (:y screen-pos)}]]))))))

;; Input field actions
(defn handle-update-handle-direct-value [store _event args]
  (let [[node-id handle-id event-or-value] args
        current-state (get @store prefix {})
        nodes-map (get current-state :nodes {})
        node (get nodes-map node-id)]
    (when node
      (let [new-value (if (instance? js/Event event-or-value)
                       (.-value (.-target event-or-value))
                       event-or-value)
            handle-data-map (get-in node [:data :handle-data] {})
            handle-data (get handle-data-map handle-id {})
            updated-handle-data (assoc handle-data :direct-value new-value)
            updated-handle-data-map (assoc handle-data-map handle-id updated-handle-data)
            updated-node-data (assoc-in (or (:data node) {}) [:handle-data] updated-handle-data-map)
            updated-node (assoc node :data updated-node-data)]
        [[:store/update-in [prefix :nodes]
          (fn [nodes]
            (assoc (or nodes {}) node-id updated-node))]]))))

(defn handle-focus-handle-input [_store _event _args]
  ;; Can be used for UI feedback if needed
  nil)

(defn handle-blur-handle-input [_store _event _args]
  ;; Can be used for UI feedback if needed
  nil)

(defn handle-disconnect-handle [store _event args]
  (let [[node-id handle-id] args
        current-state (get @store prefix {})
        edges (get current-state :edges [])
        nodes-map (get current-state :nodes {})
        node (get nodes-map node-id)]
    (when node
      ;; Find edge connected to this handle
      (let [connected-edge (first (filter (fn [edge]
                                          (and (= (:target edge) node-id)
                                               (= (:targetHandle edge) handle-id)))
                                        edges))
            edge-id (when connected-edge (:id connected-edge))
            handle-data-map (get-in node [:data :handle-data] {})
            handle-data (get handle-data-map handle-id {})
            preserved-direct-value (:direct-value handle-data)
            updated-handle-data (assoc handle-data
                                      :input-mode :direct
                                      :connection nil
                                      :direct-value preserved-direct-value)
            updated-handle-data-map (assoc handle-data-map handle-id updated-handle-data)
            updated-node-data (assoc-in (or (:data node) {}) [:handle-data] updated-handle-data-map)
            updated-node (assoc node :data updated-node-data)]
        (if edge-id
          ;; Delete edge and update handle
          [[:canvas/delete-edge edge-id]
           [:store/update-in [prefix :nodes]
            (fn [nodes]
              (assoc (or nodes {}) node-id updated-node))]]
          ;; Just update handle (edge might already be deleted)
          [[:store/update-in [prefix :nodes]
            (fn [nodes]
              (assoc (or nodes {}) node-id updated-node))]])))))

;; Main dispatcher
(defn execute-action
  "Execute canvas-related actions. Returns nil or a vector of actions."
  [store event action args]
  (case action
    ;; Node actions
    :canvas/add-node (handle-add-node store event args)
    :canvas/update-node (handle-update-node store event args)
    :canvas/delete-node (handle-delete-node store event args)
    
    ;; Edge actions
    :canvas/add-edge (handle-add-edge store event args)
    :canvas/delete-edge (handle-delete-edge store event args)
    
    ;; Selection actions
    :canvas/select (handle-select store event args)
    
    ;; Viewport actions
    :canvas/pan (handle-pan store event args)
    :canvas/zoom (handle-zoom store event args)
    :canvas/set-viewport (handle-set-viewport store event args)
    :canvas/canvas-mouse-down (handle-canvas-mouse-down store event args)
    :canvas/canvas-wheel (handle-canvas-wheel store event args)
    :canvas/canvas-touch-start (handle-canvas-touch-start store event args)
    :canvas/canvas-touch-move (handle-canvas-touch-move store event args)
    :canvas/canvas-touch-end (handle-canvas-touch-end store event args)
    
    ;; Dragging actions
    :canvas/node-mouse-down (handle-node-mouse-down store event args)
    :canvas/mouse-move (handle-mouse-move store event args)
    :canvas/mouse-up (handle-mouse-up store event args)
    :canvas/end-drag (handle-end-drag store event args)
    
    ;; Connection actions
    :canvas/handle-mouse-down (handle-handle-mouse-down store event args)
    :canvas/handle-mouse-up (handle-handle-mouse-up store event args)
    :canvas/complete-connection (handle-complete-connection store event args)
    :canvas/update-connection-position (handle-update-connection-position store event args)
    :canvas/cancel-connection (handle-cancel-connection store event args)
    
    ;; Tooltip actions
    :canvas/handle-mouse-enter (handle-handle-mouse-enter store event args)
    :canvas/handle-mouse-leave (handle-handle-mouse-leave store event args)
    :canvas/tooltip-mouse-enter (handle-tooltip-mouse-enter store event args)
    :canvas/tooltip-mouse-leave (handle-tooltip-mouse-leave store event args)
    :canvas/update-tooltip-position (handle-update-tooltip-position store event args)
    
    ;; Edge selection
    :canvas/edge-click (handle-edge-click store event args)
    :canvas/delete-selected-edges (handle-delete-selected-edges store event args)
    :canvas/delete-selected-if-key (handle-delete-selected-if-key store event args)
    
    ;; Input field actions
    :canvas/update-handle-direct-value (handle-update-handle-direct-value store event args)
    :canvas/focus-handle-input (handle-focus-handle-input store event args)
    :canvas/blur-handle-input (handle-blur-handle-input store event args)
    :canvas/disconnect-handle (handle-disconnect-handle store event args)
    
    ;; Measurement actions
    :canvas/measure-handle-offsets (handle-measure-handle-offsets store event args)
    
    ;; Default: return nil (action not handled)
    nil))

