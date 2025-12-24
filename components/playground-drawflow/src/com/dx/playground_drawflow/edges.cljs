(ns com.dx.playground-drawflow.edges
  (:require
   [com.dx.playground-drawflow.viewport :as viewport]
   [com.dx.playground-drawflow.nodes :as nodes]))

(defn calculate-edge-path [source-pos target-pos edge-type]
  (let [{:keys [sx sy]} source-pos
        {:keys [tx ty]} target-pos]
    (case (or edge-type "bezier")
      "straight"
      (str "M " sx " " sy " L " tx " " ty)
      
      "bezier"
      (let [dx (- tx sx)
            cp1x (+ sx (* dx 0.5))
            cp1y sy
            cp2x (+ sx (* dx 0.5))
            cp2y ty]
        (str "M " sx " " sy " C " cp1x " " cp1y ", " cp2x " " cp2y ", " tx " " ty))
      
      "step"
      (let [mid-x (/ (+ sx tx) 2)]
        (str "M " sx " " sy " L " mid-x " " sy " L " mid-x " " ty " L " tx " " ty))
      
      ;; Default to bezier
      (let [dx (- tx sx)
            cp1x (+ sx (* dx 0.5))
            cp1y sy
            cp2x (+ sx (* dx 0.5))
            cp2y ty]
        (str "M " sx " " sy " C " cp1x " " cp1y ", " cp2x " " cp2y ", " tx " " ty)))))

(defn get-node-center [node]
  ;; Get node center in canvas coordinates
  ;; Since SVG overlay now has the same transform as nodes-container,
  ;; we can use canvas coordinates directly
  (let [{:keys [x y]} (:position node)
        node-width nodes/default-node-width
        node-height nodes/default-node-height]
    {:x (+ x (/ node-width 2))
     :y (+ y (/ node-height 2))}))

(defn calculate-offset
  "Calculate handle position relative to node top-left corner."
  [handle-elem node-rect]
  (let [handle-rect (.getBoundingClientRect handle-elem)
        handle-center-x (+ (.-left handle-rect) (/ (.-width handle-rect) 2))
        handle-center-y (+ (.-top handle-rect) (/ (.-height handle-rect) 2))
        ;; Offset from node's top-left corner
        offset-x (- handle-center-x (.-left node-rect))
        offset-y (- handle-center-y (.-top node-rect))]
    {:x offset-x :y offset-y}))

(defn measure-handle-offsets
  "Measure handle offsets relative to their node containers.
   Returns: {node-id {handle-id {handle-type {:x offset-x :y offset-y}}}}
   "
  [nodes-map]
  (reduce (fn [acc [node-id node]]
            (let [node-elem (.getElementById js/document (str "node-" node-id))
                  node-rect (when node-elem (.getBoundingClientRect node-elem))
                  handles (get-in node [:data :handles] {})
                  all-handle-ids (concat (:source handles) (:target handles))]
              (if node-rect
                (reduce (fn [node-acc handle-id]
                          (let [source-elem-id (str "handle-" node-id "-source-" handle-id)
                                target-elem-id (str "handle-" node-id "-target-" handle-id)
                                source-elem (.getElementById js/document source-elem-id)
                                target-elem (.getElementById js/document target-elem-id)]
                            (cond-> node-acc
                              source-elem
                              (assoc-in [node-id handle-id :source]
                                       (calculate-offset source-elem node-rect))
                              target-elem
                              (assoc-in [node-id handle-id :target]
                                       (calculate-offset target-elem node-rect)))))
                        acc
                        all-handle-ids)
                acc)))
          {}
          nodes-map))

(defn get-handle-position-in-canvas
  "Get handle position in canvas coordinates.
   Calculates: node.position + handle.offset = canvas position
   Falls back to node center if offset not available.
   "
  [node handle-id handle-type handle-offsets]
  (if-let [offset (get-in handle-offsets [(:id node) handle-id handle-type])]
    ;; Calculate absolute position: node position + relative offset
    {:x (+ (:x (:position node)) (:x offset))
     :y (+ (:y (:position node)) (:y offset))}
    ;; Fallback to node center
    (get-node-center node)))

(defn get-handle-position 
  "Get handle position in canvas coordinates (legacy function for backward compatibility).
   Now uses measured offsets if available, otherwise falls back to calculated position.
   "
  [node handle-type handle-id handle-offsets]
  (if handle-offsets
    (get-handle-position-in-canvas node handle-id handle-type handle-offsets)
    ;; Fallback to old calculation method
    (let [handle-pos (nodes/get-handle-position node handle-type handle-id)]
      {:x (:x handle-pos) :y (:y handle-pos)})))

(defn render-edge [edge nodes handle-offsets]
  (let [{:keys [id source target type selected? sourceHandle targetHandle]} edge
        source-node (get nodes source)
        target-node (get nodes target)]
    (when (and source-node target-node)
      (let [;; Use handle positions if available, otherwise use node centers
            source-pos-data (if sourceHandle
                             (get-handle-position-in-canvas source-node sourceHandle :source handle-offsets)
                             (get-node-center source-node))
            target-pos-data (if targetHandle
                             (get-handle-position-in-canvas target-node targetHandle :target handle-offsets)
                             (get-node-center target-node))
            source-pos {:sx (:x source-pos-data) :sy (:y source-pos-data)}
            target-pos {:tx (:x target-pos-data) :ty (:y target-pos-data)}
            path-d (calculate-edge-path source-pos target-pos type)
            is-selected (boolean selected?)]
        [:path.edge
         {:id (str "edge-" id)
          :d path-d
          :fill "none"
          :stroke (if is-selected "#3b82f6" "#999")
          :stroke-width (if is-selected "3" "2")
          :marker-end "url(#arrowhead)"
          :style {:cursor "pointer"
                  :pointer-events "stroke"}
          :on {:click [[:canvas/edge-click id :event/event]]}}]))))

(defn render-temp-connection
  "Render temporary connection line when dragging from handle"
  [connection nodes _viewport handle-offsets]
  (when (and connection (= (:type connection) :edge))
    (let [source-node (get nodes (:source-node connection))
          target-pos (:position connection)]
      (when source-node
        (let [source-handle-pos (if (:source-handle connection)
                                  (get-handle-position-in-canvas source-node (:source-handle connection) :source handle-offsets)
                                  (get-node-center source-node))
              source-pos {:sx (:x source-handle-pos) :sy (:y source-handle-pos)}
              target-pos-data {:tx (:x target-pos) :ty (:y target-pos)}
              path-d (calculate-edge-path source-pos target-pos-data "bezier")]
          [:path.temp-connection
           {:d path-d
            :fill "none"
            :stroke "#3b82f6"
            :stroke-width "2"
            :stroke-dasharray "5,5"
            :opacity "0.7"
            :pointer-events "none"}])))))

(defn render-edges-overlay [edges nodes viewport connection handle-offsets]
  [:svg.edges-overlay
     {:style (merge (viewport/viewport-style viewport)
                    {:width "100%"
                     :height "100%"
                     :z-index 1})
      :pointer-events "stroke"}
     
     ;; SVG defs for markers (arrows)
     [:defs
      [:marker#arrowhead
       {:marker-width "10"
        :marker-height "10"
        :ref-x "9"
        :ref-y "3"
        :orient "auto"}
       [:polygon
        {:points "0 0, 10 3, 0 6"
         :fill "#999"}]]]
     
     ;; Render all edges
     (map (fn [edge]
            ^{:key (:id edge)}
            (render-edge edge nodes handle-offsets))
          edges)
     
     ;; Render temporary connection line
     (render-temp-connection connection nodes viewport handle-offsets)])

