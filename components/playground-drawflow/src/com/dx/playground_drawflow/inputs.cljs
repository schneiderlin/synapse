(ns com.dx.playground-drawflow.inputs)

(defn default-input-component
  "Default input component for direct input mode.
   
   Args:
   - node-id: The node ID
   - handle-id: The handle ID
   - handle-type: :source or :target (should be :target for inputs)
   - value: Current direct value
   - _on-change: Function to call when value changes (unused, using action directly)
   - input-type: Optional input type (default: \"text\")
   
   Returns hiccup for input field.
   "
  [node-id handle-id _handle-type value _on-change & [input-type]]
  [:input.input.input-sm.input-bordered.w-full.max-w-xs
   {:type (or input-type "text")
    :value (or value "")
    :placeholder "Enter value..."
    :style {:font-size "12px"
            :padding "4px 8px"}
    :on {:change [[:canvas/update-handle-direct-value node-id handle-id :event/event]]
         :focus [[:canvas/focus-handle-input node-id handle-id]]
         :blur [[:canvas/blur-handle-input node-id handle-id]]}}])

(defn default-connection-info-component
  "Default component for displaying connection information.
   
   Args:
   - node-id: The target node ID
   - handle-id: The target handle ID
   - connection: Map with :source-node-id and :source-handle-id
   - _on-disconnect: Function to call when disconnect button is clicked (unused, using action directly)
   - _source-node: Optional source node data (for value preview)
   
   Returns hiccup for connection info display.
   "
  [node-id handle-id connection _on-disconnect & [_source-node]]
  (let [source-node-id (:source-node-id connection)
        source-handle-id (:source-handle-id connection)]
    [:div.flex.items-center.gap-2.w-full
     {:style {:font-size "12px"}}
     [:span
      {:class ["text-sm" "text-base-content/70"]
       :style {:flex "1"
               :overflow "hidden"
               :text-overflow "ellipsis"
               :white-space "nowrap"}}
      (str source-node-id ":" source-handle-id)]
     [:button.btn.btn-xs.btn-ghost.btn-circle
      {:title "Disconnect"
       :style {:padding "0"
               :min-height "16px"
               :height "16px"
               :width "16px"
               :flex-shrink 0}
       :on {:click [[:canvas/disconnect-handle node-id handle-id]]}}
      [:span {:style {:font-size "12px"
                      :line-height "1"}} "✕"]]]))

(defn get-handle-input-mode
  "Get the input mode for a handle.
   Returns :connected, :direct, or nil (defaults to :direct if not set).
   
   Args:
   - node: The node data
   - handle-id: The handle ID
   "
  [node handle-id]
  (let [handle-data-map (get-in node [:data :handle-data] {})
        handle-data (get handle-data-map handle-id {})
        input-mode (:input-mode handle-data)]
    (or input-mode :direct)))

(defn get-handle-direct-value
  "Get the direct value for a handle.
   
   Args:
   - node: The node data
   - handle-id: The handle ID
   "
  [node handle-id]
  (let [handle-data-map (get-in node [:data :handle-data] {})
        handle-data (get handle-data-map handle-id {})]
    (:direct-value handle-data)))

(defn get-handle-connection
  "Get the connection info for a handle.
   
   Args:
   - node: The node data
   - handle-id: The handle ID
   "
  [node handle-id]
  (let [handle-data-map (get-in node [:data :handle-data] {})
        handle-data (get handle-data-map handle-id {})]
    (:connection handle-data)))

(defn resolve-handle-value
  "Resolve the value for a handle.
   Returns the value based on input mode:
   - :connected -> get value from source node
   - :direct -> return direct-value
   - If both exist, prefer connection (can be extended for override mode)
   
   Args:
   - node: The target node data
   - handle-id: The handle ID
   - nodes-map: Map of all nodes (for resolving connected values)
   
   Returns the resolved value or nil.
   "
  [node handle-id nodes-map]
  (let [input-mode (get-handle-input-mode node handle-id)]
    (case input-mode
      :connected
      (let [connection (get-handle-connection node handle-id)]
        (when connection
          (let [source-node-id (:source-node-id connection)
                source-handle-id (:source-handle-id connection)
                source-node (get nodes-map source-node-id)]
            (when source-node
              ;; For now, return the source node's output value
              ;; This can be extended to get actual output values from source node
              ;; For now, we'll return a placeholder or the source node's data
              (get-handle-direct-value source-node source-handle-id)))))
      
      :direct
      (get-handle-direct-value node handle-id)
      
      ;; Default: return direct value if exists
      (get-handle-direct-value node handle-id))))

(defn create-input-helper
  "Create an input rendering helper function for body components.
   Returns a function that body components can call to render input fields for target handles.
   
   Usage in body component:
   (render-input-helper \"input1\")
   
   The returned hiccup can be positioned inline in your node body using normal HTML/CSS.
   No automatic positioning is applied - you control the layout.
   
   Args:
   - node: The node data
   - edges: Vector of all edges (for checking connections)
   - nodes-map: Map of all nodes (for resolving connected values)
   
   Returns a function that takes:
   - handle-id: String identifier for the handle
   "
  [node edges nodes-map]
  (fn [handle-id]
    (let [{:keys [id]} node
          handle-type :target  ; Inputs are always for target handles
          handle-data-map (get-in node [:data :handle-data] {})
          handle-data (get handle-data-map handle-id {})
          ;; Find edge connected to this handle (source of truth for connection)
          connected-edge (first (filter (fn [edge]
                                         (and (= (:target edge) id)
                                              (= (:targetHandle edge) handle-id)))
                                       edges))
          ;; Determine input mode: if edge exists, use connected mode; otherwise use handle data
          input-mode (if connected-edge
                      :connected
                      (get-handle-input-mode node handle-id))
          direct-value (get-handle-direct-value node handle-id)
          ;; Get connection info from edge or handle data
          connection (if connected-edge
                      {:source-node-id (:source connected-edge)
                       :source-handle-id (:sourceHandle connected-edge)}
                      (get-handle-connection node handle-id))
          ;; Get source node if connected
          source-node (when (and connected-edge nodes-map)
                       (get nodes-map (:source connected-edge)))
          ;; Get input component (custom or default)
          input-component (or (:input-component handle-data)
                            default-input-component)
          ;; Get connection info component (custom or default)
          connection-info-component (or (:connection-info-component handle-data)
                                      default-connection-info-component)]
      ;; Return hiccup for input component or connection info component
      ;; No position wrapper - developer controls positioning
      (case input-mode
        :connected
        (connection-info-component id handle-id connection
                                  (fn [] [[:canvas/disconnect-handle id handle-id]])
                                  source-node)
        
        :direct
        (input-component id handle-id handle-type direct-value
                        (fn [new-value] [[:canvas/update-handle-direct-value id handle-id new-value]]))
        
        ;; Default: show input field
        (input-component id handle-id handle-type direct-value
                        (fn [new-value] [[:canvas/update-handle-direct-value id handle-id new-value]]))))))
