(ns com.dx.playground-drawflow.nodes
  (:require
   [com.dx.playground-drawflow.tooltips :as tooltips]
   [com.dx.playground-drawflow.inputs :as inputs]))

(def default-node-width 150)
(def default-node-height 50)
(def handle-size 12)
(def handle-offset 6)

(defn get-handle-position 
  "Get handle position in canvas coordinates.
   If position is provided in node data, use it; otherwise use default positions.
   Position can be:
   - Map with :x and :y as pixels or percentages (e.g., {:x \"100%\" :y \"50%\"} or {:x 150 :y 25})
   - nil (uses default based on handle-type)
   
   Handle positions can be stored in :data :handle-data as:
   {:handle-data {\"handle-id\" {:position {:x \"100%\" :y \"50%\"}}}}
   "
  [node handle-type handle-id]
  (let [{:keys [x y]} (:position node)
        node-width default-node-width
        node-height default-node-height
        ;; Check if handle has custom position in node data
        ;; Handle positions stored in :data :handle-data as map of handle-id to handle metadata
        handle-data-map (get-in node [:data :handle-data] {})
        handle-data (get handle-data-map handle-id {})
        custom-pos (:position handle-data)]
    (if custom-pos
      ;; Custom position - convert to canvas coordinates
      (let [pos-x (:x custom-pos)
            pos-y (:y custom-pos)
            ;; Handle percentage or pixel values
            abs-x (if (string? pos-x)
                    (if (.endsWith pos-x "%")
                      (* node-width (/ (js/parseFloat pos-x) 100))
                      (js/parseFloat pos-x))
                    pos-x)
            abs-y (if (string? pos-y)
                    (if (.endsWith pos-y "%")
                      (* node-height (/ (js/parseFloat pos-y) 100))
                      (js/parseFloat pos-y))
                    pos-y)]
        {:x (+ x abs-x) :y (+ y abs-y)})
      ;; Default position based on handle type
      (case handle-type
        :source
        {:x (+ x node-width) :y (+ y (/ node-height 2))}
        :target
        {:x x :y (+ y (/ node-height 2))}
        nil))))

(defn get-handle-position-relative
  "Get handle position relative to the node (for rendering).
   If position is provided, use it; otherwise use default positions.
   
   Handle positions can be stored in :data :handle-data as:
   {:handle-data {\"handle-id\" {:position {:x \"100%\" :y \"50%\"}}}}
   "
  [node handle-type handle-id]
  (let [node-width default-node-width
        node-height default-node-height
        ;; Check if handle has custom position in node data
        ;; Handle positions stored in :data :handle-data as map of handle-id to handle metadata
        handle-data-map (get-in node [:data :handle-data] {})
        handle-data (get handle-data-map handle-id {})
        custom-pos (:position handle-data)]
    (if custom-pos
      ;; Custom position - return as-is (already relative)
      (let [pos-x (:x custom-pos)
            pos-y (:y custom-pos)
            ;; Handle percentage or pixel values
            rel-x (if (string? pos-x)
                    (if (.endsWith pos-x "%")
                      (* node-width (/ (js/parseFloat pos-x) 100))
                      (js/parseFloat pos-x))
                    pos-x)
            rel-y (if (string? pos-y)
                    (if (.endsWith pos-y "%")
                      (* node-height (/ (js/parseFloat pos-y) 100))
                      (js/parseFloat pos-y))
                    pos-y)]
        {:x rel-x :y rel-y})
      ;; Default position based on handle type
      (case handle-type
        :source
        {:x node-width :y (/ node-height 2)}
        :target
        {:x 0 :y (/ node-height 2)}
        nil))))

(defn create-handle-helper
  "Create a handle rendering helper function for body components.
   Returns a function that body components can call to render handles.
   
   Usage in body component:
   (render-handle-helper \"output1\" :source)
   (render-handle-helper \"input1\" :target {:class \"custom-class\" :style {:margin \"4px\"}})
   
   The returned hiccup can be positioned inline in your node body using normal HTML/CSS.
   No automatic positioning is applied - you control the layout.
   
   Args:
   - node: The node data
   - connection: Current connection state (for highlighting)
   - edges: Vector of all edges (for checking connections)
   - tooltip-state: Current tooltip state (for rendering tooltips)
   
   Returns a function that takes:
   - handle-id: String identifier for the handle
   - handle-type: :source or :target
   - opts: Optional map with :class, :style, etc. (for extending styling, NOT for position)
   "
  [node connection edges tooltip-state]
  (fn [handle-id handle-type & [opts]]
    (let [{:keys [id]} node
          is-connecting (and connection
                           (= (:type connection) :edge)
                           (= (:source-node connection) id)
                           (= (:source-handle connection) handle-id))
          ;; Check if handle is connected (for visual styling)
          is-connected (and (= handle-type :target)
                           (some (fn [edge]
                                  (and (= (:target edge) id)
                                       (= (:targetHandle edge) handle-id)))
                                edges))
          ;; Check if handle has direct value (for visual styling)
          handle-data-map (get-in node [:data :handle-data] {})
          handle-data (get handle-data-map handle-id {})
          input-mode (or (:input-mode handle-data) :direct)
          has-direct-value (and (= input-mode :direct)
                               (some? (:direct-value handle-data)))
          ;; Determine handle visual style
          handle-style (cond
                        is-connecting {:background-color "#3b82f6"
                                      :border "2px solid #1e40af"}
                        is-connected {:background-color "#3b82f6"
                                     :border "2px solid #1e40af"}
                        has-direct-value {:background-color "#10b981"
                                         :border "2px solid #059669"}
                        :else {:background-color "#fff"
                              :border "2px solid #999"})
          ;; Merge user-provided opts
          user-class (:class opts)
          user-style (:style opts)
          ;; Check if this handle should show tooltip
          show-tooltip? (and tooltip-state
                            (= (:node-id tooltip-state) id)
                            (= (:handle-id tooltip-state) handle-id)
                            (= (:handle-type tooltip-state) handle-type))
          tooltip-content (when show-tooltip?
                           (tooltips/render-tooltip tooltip-state node))]
      ;; Wrap handle and tooltip in relative container so tooltip can be positioned relative to handle
      [:div.handle-wrapper
       {:style {:position "relative"
                :display "inline-block"}}
       [:div.handle
        {:id (str "handle-" id "-" (name handle-type) "-" handle-id)
         :key (str "handle-" id "-" (name handle-type) "-" handle-id)
         :class (cond-> ["handle" (str "handle-" (name handle-type))
                        (when is-connecting "handle-connecting")
                        (when is-connected "handle-connected")
                        (when has-direct-value "handle-direct-value")]
                 user-class (conj user-class))
         :style (merge {:width (str handle-size "px")
                       :height (str handle-size "px")
                       :border-radius "50%"
                       :cursor "crosshair"
                       :pointer-events "auto"
                       :box-shadow "0 2px 4px rgba(0,0,0,0.2)"
                       :flex-shrink 0}
                      handle-style
                      user-style)
         :on {:mousedown [[:canvas/handle-mouse-down id handle-type handle-id :event/event]]
              :mouseup [[:canvas/handle-mouse-up id handle-type handle-id :event/event]]
              :mouseenter [[:canvas/handle-mouse-enter id handle-type handle-id :event/event]]
              :mouseleave [[:canvas/handle-mouse-leave id handle-type handle-id :event/event]]}}]
       ;; Render tooltip positioned relative to handle
       (when show-tooltip?
         [:div.tooltip-container
          {:style {:position "absolute"
                   :left (str tooltips/tooltip-offset-x "px")
                   :top (str tooltips/tooltip-offset-y "px")
                   :transform "translateX(-50%)"
                   :z-index 1000
                   :pointer-events "auto"}}
          tooltip-content])])))

(defn default-node-body
  "Default node body component.
   This demonstrates how to use the handle helper and input helper in a body component.
   
   Args:
   - node: The node data
   - _connection: Current connection state (unused in default, but part of signature for consistency)
   - render-handle-helper: Function to render handles (created by create-handle-helper)
   - render-input-helper: Function to render input fields (created by create-input-helper)
   "
  [node _connection render-handle-helper render-input-helper]
  (let [node-data (:data node)
        is-selected (boolean (:selected? node))
        ;; Get handles from node data, or use defaults
        handles (or (:handles node-data) {:source ["output"] :target ["input"]})]
    [:div
     ;; Node header
     [:div.node-header
      {:style {:font-weight "bold"
               :margin-bottom "8px"
               :border-bottom "1px solid #eee"
               :padding-bottom "4px"
               :color (if is-selected "#1e40af" "inherit")}}
      (or (:label node-data) (str "Node " (:id node)))]
     
     ;; Node body content
     [:div.node-body
      {:style {:color (if is-selected "#1e3a8a" "inherit")}}
      (or (:content node-data) "Node content")]
     
     ;; Input fields for target handles (rendered before handles so handles appear on top)
     (map (fn [handle-id]
            ^{:key (str "input-" handle-id)}
            (render-input-helper handle-id))
          (:target handles))
     
     ;; Connection handles - rendered by body component
     (map (fn [handle-id]
            ^{:key (str "source-" handle-id)}
            (render-handle-helper handle-id :source))
          (:source handles))
     (map (fn [handle-id]
            ^{:key (str "target-" handle-id)}
            (render-handle-helper handle-id :target))
          (:target handles))]))

(defn render-node
  "Render a node with optional custom body component.
   
   The body component can be specified in node data as :body-component.
   If not provided, uses default-node-body.
   
   Body component signature:
   (fn [node connection render-handle-helper render-input-helper])
   
   Where:
   - render-handle-helper is a function created by create-handle-helper
     that can be called to render handles:
     (render-handle-helper handle-id handle-type & [opts])
     The returned hiccup can be positioned inline in your node body using normal HTML/CSS.
     Handles render their own tooltips when active.
   - render-input-helper is a function created by create-input-helper
     that can be called to render input fields for target handles:
     (render-input-helper handle-id)
     The returned hiccup can be positioned inline in your node body using normal HTML/CSS.
   "
  [node dragging connection tooltip-state edges nodes-map]
  (let [{:keys [id type position data selected?]} node
        {:keys [x y]} (or position {:x 0 :y 0})
        node-type (or type "default")
        node-data (or data {})
        is-selected (boolean selected?)
        is-dragging (and dragging (= (:node-id dragging) id))
        ;; Get body component from node data, or use default
        body-component (or (:body-component node-data) default-node-body)
        ;; Create handle helper for body component (pass tooltip-state so handles can render tooltips)
        render-handle-helper (create-handle-helper node connection edges tooltip-state)
        ;; Create input helper for body component
        render-input-helper (inputs/create-input-helper node edges nodes-map)]
    [:div.node
     {:id (str "node-" id)
      :class ["node" 
              (str "node-" node-type)
              (when is-selected "node-selected")
              (when is-dragging "node-dragging")]
      :style {:position "absolute"
              :left (str x "px")
              :top (str y "px")
              :min-width (str default-node-width "px")
              :min-height (str default-node-height "px")
              :background-color (if is-selected "#f0f9ff" "white")
              :border (if is-selected "2px solid #3b82f6" "2px solid #ddd")
              :border-radius "8px"
              :padding "12px"
              :box-shadow (if is-selected 
                           "0 4px 8px rgba(59, 130, 246, 0.3)" 
                           "0 2px 4px rgba(0,0,0,0.1)")
              :cursor "move"
              :user-select "none"
              :transition (when-not is-dragging "box-shadow 0.2s, border-color 0.2s")}
      :on {:mousedown [[:canvas/node-mouse-down id :event/event]]}}
     
     ;; Render body component (handles and inputs are rendered inside body component)
     ;; Handles render their own tooltips when active
     (body-component node connection render-handle-helper render-input-helper)]))
