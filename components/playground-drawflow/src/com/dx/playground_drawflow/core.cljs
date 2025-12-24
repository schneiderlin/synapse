(ns com.zihao.playground-drawflow.core
  (:require
   [com.zihao.playground-drawflow.nodes :as nodes]
   [com.zihao.playground-drawflow.edges :as edges]
   [com.zihao.playground-drawflow.viewport :as viewport]))

(defn render-canvas [state]
  (let [canvas-state (get state :playground-drawflow/canvas {})
        nodes-map (get canvas-state :nodes {})
        nodes (vals nodes-map)
        edges (get canvas-state :edges [])
        viewport (get canvas-state :viewport {:x 0 :y 0 :zoom 1})
        dragging (get canvas-state :dragging nil)
        connection (get canvas-state :connection nil)
        tooltip-state (get canvas-state :tooltip nil)
        handle-offsets (get canvas-state :handle-offsets {})]
    [:div.canvas-container
     {:style {:position "relative"
              :width "100%"
              :height "100%"
              :overflow "hidden"
              :background-color "#f5f5f5"
              :cursor (cond
                       (and dragging (= (:type dragging) :pan)) "grabbing"
                       (not dragging) "grab"
                       :else "default")}
      :on {:mousedown [[:canvas/canvas-mouse-down :event/event]]
           :mousemove [[:canvas/mouse-move :event/event]]
           :mouseup [[:canvas/mouse-up :event/event]]
           :wheel [[:canvas/canvas-wheel :event/event]]
           :touchstart [[:canvas/canvas-touch-start :event/event]]
           :touchmove [[:canvas/canvas-touch-move :event/event]]
           :touchend [[:canvas/canvas-touch-end :event/event]]}}
     
     ;; CSS for tooltip animation
     [:style
      "@keyframes tooltip-fade-in {
         from { 
           opacity: 0; 
           transform: translateY(-4px) scale(0.95); 
         }
         to { 
           opacity: 1; 
           transform: translateY(0) scale(1); 
         }
       }"]
     
     ;; Background grid (optional)
     [:div.background-grid
      {:style {:position "absolute"
               :top 0
               :left 0
               :width "100%"
               :height "100%"
               :background-image "radial-gradient(circle, #ddd 1px, transparent 1px)"
               :background-size "20px 20px"
               :pointer-events "none"
               :z-index 0}}]
     
     ;; SVG overlay for edges (behind nodes)
     (edges/render-edges-overlay edges nodes-map viewport connection handle-offsets)
     
     ;; Nodes container with viewport transform
     [:div.nodes-container
      {:style (viewport/viewport-style viewport)}
      (map (fn [node]
             ^{:key (:id node)}
             (nodes/render-node node dragging connection tooltip-state edges nodes-map))
           nodes)]]))

