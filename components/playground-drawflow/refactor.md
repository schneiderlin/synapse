# Refactor Plan: Simplify Input and Handle Positioning

## Overview
Remove automatic position calculations for inputs and handles. Let node developers control positioning directly in their node body components using normal HTML/CSS flow, similar to how they already control handle positions.

## Problem Statement

### Current Issues
1. **Complex position calculations**: `create-input-helper` automatically calculates input field positions relative to handles, requiring complex coordinate math
2. **Unnecessary abstraction**: Both helper APIs currently try to own layout; node authors just want hiccup that they can drop anywhere
3. **Rigid positioning**: Automatic calculations limit flexibility - devs can't easily place inputs/handles next to specific elements in their custom layouts
4. **Duplicated logic**: `calculate-handle-position-relative` in `inputs.cljs` duplicates logic from `nodes.cljs` to avoid circular dependencies

### Goal
- **Simplify**: Remove all automatic position calculations
- **Empower developers**: Let them position inputs and handles inline in their node body using normal HTML/CSS
- **Consistency**: Make inputs work the same way as handles - just call the helper and render it where you want

## Architecture Changes

### Principle: Inline Rendering
Both handles and inputs should be rendered inline in the node body, positioned by the developer using:
- Normal HTML flow (flexbox, grid, etc.)
- CSS positioning (relative, absolute within node body)
- CSS transforms
- Any other CSS technique

No automatic calculations needed - the developer controls everything.

## Refactor Plan

### Phase 1: Simplify `create-input-helper`

#### Current Behavior
```clojure
(render-input-helper "input1" {:x "0%" :y "50%"})
;; Automatically calculates position relative to handle
;; Wraps in absolutely positioned div
;; Returns hiccup with calculated position
```

#### New Behavior
```clojure
(render-input-helper "input1")
;; Returns hiccup for input component (no position wrapper)
;; Developer positions it inline in their node body
;; No position parameter needed
```

#### Changes Needed
- [x] **1.1** Remove position parameter from `create-input-helper`
  - Function signature: `(render-input-helper handle-id)` (no position)
  - Remove `calculate-handle-position-relative` call
  - Remove position calculation logic (lines 242-251 in inputs.cljs)

- [x] **1.2** Remove absolute positioning wrapper
  - Don't wrap input in `[:div.handle-input-wrapper]` with absolute positioning
  - Return just the input/connection-info component directly
  - Let developer wrap it however they want

- [x] **1.3** Simplify return value
  - Return hiccup for input component or connection info component
  - No position styling - developer controls that
  - Keep all the logic for determining input mode, getting values, etc.

### Phase 2: Redefine `create-handle-helper`

#### Current Behavior
```clojure
(render-handle-helper "output1" :source {:x "100%" :y "50%"})
;; Requires manual position overrides
;; Always renders as absolutely positioned circle inside node
;; Helper owns layout, caller can only tweak via :position map
```

#### New Behavior
```clojure
(render-handle-helper "output1" :source)
;; Returns a ready-to-use hiccup snippet (button/div) with wiring + base styling
;; No :position parameter
;; Caller decides where to place it (inline, flexbox, absolute, etc.)
```

#### Changes Needed
- [x] **2.1** Remove position parameter entirely
  - Signature becomes `(render-handle-helper handle-id handle-type & [opts])`
  - `opts` limited to behavior/styling flags (e.g., `:label`, `:class`, `:style`), **not coordinates**
  - Helper no longer calls `get-handle-position-relative`

- [x] **2.2** Return plain hiccup without internal absolute positioning
  - Use a semantic element (e.g., `[:button.handle ...]`) with default size/border/hover styles
  - Allow caller to extend via `:class` / `:style` passed in `opts`
  - Provide minimal inline styles to ensure it still looks/behaves like a handle out of the box

- [x] **2.3** Move layout responsibility to caller
  - Document that callers must place handles in the right spot relative to their node content
  - Encourage using flexbox/grid or absolute positioning inside the body component

- [x] **2.4** Refactor tooltip architecture
  - `render-tooltip` should return hiccup with **no position information** (just content)
  - Node determines what the tooltip looks like (via tooltip component), not where it's positioned
  - Tooltip content hiccup should be passed into the handle
  - Handle positions the tooltip relative to itself using `tooltip-offset-x` and `tooltip-offset-y`
  - Remove tooltip rendering from node level - handles render their own tooltips

### Phase 2.5: Update Edge Rendering to Use DOM Measurements

#### Problem
After Phase 1 and 2, handles are positioned by developers using CSS (flexbox, absolute, etc.). The current edge rendering system (`edges.cljs`) relies on `nodes/get-handle-position` which calculates positions from node data or defaults. This won't work anymore because:
1. Handles can be positioned anywhere in the node body using any CSS technique
2. We can't predict handle positions from node data alone
3. We need to measure actual DOM positions to render edges correctly

#### Current Edge Rendering Flow
```clojure
;; edges.cljs - render-edge function
(get-handle-position source-node :source sourceHandle)
;; ↓ calls nodes/get-handle-position
;; ↓ calculates from node data or defaults
;; ↓ returns canvas coordinates
;; ↓ used to draw SVG path
```

#### New Edge Rendering Flow
```clojure
;; edges.cljs - render-edge function
(get-handle-position-in-canvas node handle-id handle-type handle-offsets)
;; ↓ reads relative offset from state :handle-offsets
;; ↓ calculates: node.position + handle.offset = canvas position
;; ↓ returns canvas coordinates
;; ↓ used to draw SVG path
```

**Key Insight**: Store **relative offsets** (handle position within node), not absolute canvas positions!
- Relative offsets don't change when node is dragged
- Relative offsets don't change when canvas is zoomed/panned
- Only need to re-measure when node layout changes (rare)

#### Changes Needed

- [ ] **2.5.1** Add handle offset measurement system
  - Create function: `measure-handle-offsets` - reads all `.handle` elements from DOM
  - For each handle, measure position relative to its node container
  - Use `getBoundingClientRect()` to get handle position and node position
  - Calculate: `offset = handle.rect - node.rect` (relative to node top-left)
  - Store results in state at `[:playground-drawflow/canvas :handle-offsets]`
  - Structure: `{node-id {handle-id {handle-type {:x offset-x :y offset-y}}}}`
  - Offsets are in pixels, relative to node's top-left corner

- [ ] **2.5.2** Add measurement trigger mechanism
  - Trigger measurements after render cycle completes
  - Use `requestAnimationFrame` to ensure DOM is ready
  - Trigger on:
    - Initial render
    - Node added/removed
    - Node body component changes (layout might have changed)
  - **NOT triggered on**: node drag, viewport pan/zoom (offsets don't change!)
  - Add action: `:canvas/measure-handle-offsets` that triggers measurement
  - Debounce/throttle for performance (measurements are rare)

- [ ] **2.5.3** Update `edges/get-handle-position` function
  - **Current**: Calls `nodes/get-handle-position` which calculates from data
  - **New**: Calculate from node position + handle offset
  - Formula: `canvas-x = node.x + handle-offset.x`, `canvas-y = node.y + handle-offset.y`
  - Fallback to node center if offset not available yet
  - Signature: `(get-handle-position-in-canvas node handle-id handle-type handle-offsets)`
  - Remove dependency on `nodes/get-handle-position` for edge rendering

- [ ] **2.5.4** Update `render-edge` function
  - Change to use calculated positions (node position + offset)
  - Pass `handle-offsets` from state to `get-handle-position-in-canvas`
  - Handle case where measurement hasn't completed yet (show edge at node center temporarily)

- [ ] **2.5.5** Update `render-temp-connection` function
  - Use calculated positions for source handle (node position + offset)
  - Mouse position for target (already working)
  - Ensure smooth dragging experience

- [ ] **2.5.6** Update `handle-handle-mouse-down` action
  - **Current**: Uses `nodes/get-handle-position` to get initial connection position
  - **New**: Calculate from node position + handle offset
  - Fallback to calculation if offset not available

- [ ] **2.5.7** Add measurement lifecycle management
  - Clear offsets when nodes are deleted
  - Re-measure when nodes are added
  - Re-measure when node body component changes (might affect layout)
  - **No need to re-measure on drag/pan/zoom** (offsets are relative!)

- [ ] **2.5.8** Handle edge cases
  - What if handle element doesn't exist in DOM yet? (fallback to node center)
  - What if node is off-screen? (can still measure if node element exists)
  - What if handle is hidden? (measure anyway, or skip?)

#### Implementation Details

**New State Structure:**
```clojure
{:playground-drawflow/canvas
 {:handle-offsets
  {"node-1" {"input1" {:target {:x 5 :y 25}}    ; 5px from left, 25px from top of node
             "output1" {:source {:x 145 :y 25}}} ; 145px from left, 25px from top
  "node-2" {"input1" {:target {:x 10 :y 40}}}
  ...}
  ...}}
```
**Note**: Offsets are relative to node's top-left corner, in pixels. They don't change when node moves or canvas transforms!

**Measurement Function:**
```clojure
(defn measure-handle-offsets [nodes-map]
  "Measure handle offsets relative to their node containers.
   Returns: {node-id {handle-id {handle-type {:x offset-x :y offset-y}}}}
   "
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

(defn calculate-offset [handle-elem node-rect]
  "Calculate handle position relative to node top-left corner."
  (let [handle-rect (.getBoundingClientRect handle-elem)
        handle-center-x (+ (.-left handle-rect) (/ (.-width handle-rect) 2))
        handle-center-y (+ (.-top handle-rect) (/ (.-height handle-rect) 2))
        ;; Offset from node's top-left corner
        offset-x (- handle-center-x (.-left node-rect))
        offset-y (- handle-center-y (.-top node-rect))]
    {:x offset-x :y offset-y}))
```

**Updated Edge Rendering:**
```clojure
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

(defn render-edge [edge nodes handle-offsets]
  (let [{:keys [id source target type selected? sourceHandle targetHandle]} edge
        source-node (get nodes source)
        target-node (get nodes target)]
    (when (and source-node target-node)
      (let [source-pos-data (if sourceHandle
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
```

**Measurement Trigger:**
```clojure
;; In core.cljs or actions.cljs
(defn trigger-handle-offset-measurement [store]
  (js/requestAnimationFrame
   (fn []
     (let [current-state (get @store prefix {})
           nodes-map (get current-state :nodes {})
           measured-offsets (measure-handle-offsets nodes-map)]
       (when (seq measured-offsets)
         (swap! store assoc-in [prefix :handle-offsets] measured-offsets))))))

;; Trigger on node add/remove, NOT on drag/pan/zoom
;; In actions.cljs:
(defn handle-add-node [store _event args]
  [...existing code...
   [:canvas/measure-handle-offsets]])  ; Trigger measurement after node added
```

#### Performance Considerations

1. **Measurement Frequency**
   - **Much simpler now!** Only measure when:
     - Nodes are added/removed
     - Node body components change (layout might have changed)
   - **NOT needed on**: node drag, viewport pan/zoom (offsets don't change!)
   - **Recommendation**: Measure after initial render and on node add/remove

2. **No Viewport Transform Needed**
   - Since we're measuring relative offsets (handle position within node), we don't need to convert screen→canvas
   - We just need: `handle-rect - node-rect` (both in screen coordinates, difference is the same)
   - Much simpler than previous approach!

3. **Off-screen Nodes**
   - If node element exists in DOM, we can measure it (even if off-screen)
   - If handle element doesn't exist, fallback to node center
   - **Recommendation**: Measure what's available, use fallback for missing

4. **Caching**
   - Offsets are cached in state and only invalidated when node layout changes
   - No need to re-measure on every frame
   - **Much more efficient** than storing absolute positions!

#### Testing Considerations

- [ ] Test edges render correctly with inline-positioned handles
- [ ] Test edges update when nodes are dragged
- [ ] Test edges update when viewport is panned/zoomed
- [ ] Test edges render correctly with custom node bodies
- [ ] Test temporary connection line during drag
- [ ] Test performance with many nodes/edges
- [ ] Test edge rendering when handles are positioned with different CSS techniques
- [ ] Test edge rendering when handles are dynamically shown/hidden

#### Migration Notes

- `nodes/get-handle-position` can remain for backward compatibility (used by tooltips, etc.)
- But edge rendering should use measured positions exclusively
- This is a breaking change for edge rendering, but transparent to node developers

### Phase 3: Update Default Node Body

#### Current Behavior
```clojure
;; Inputs rendered with automatic positioning
(map (fn [handle-id]
      (render-input-helper handle-id))
    (:target handles))
```

#### New Behavior
```clojure
;; Developer explicitly positions inputs inline
[:div.flex.items-center.gap-2
 [:span "Input:"]
 (render-input-helper "input1")
 (render-handle-helper "input1" :target)]
```

#### Changes Needed
- [ ] **3.1** Update `default-node-body` to demonstrate inline input positioning
  - Show inputs next to labels or other elements
  - Use flexbox/grid for layout
  - Position handles relative to inputs

- [ ] **3.2** Update examples in documentation
  - Show how to create custom layouts with inputs and handles
  - Demonstrate inline positioning patterns

### Phase 4: Remove Duplicated Code

#### Changes Needed
- [ ] **4.1** Remove `calculate-handle-position-relative` from `inputs.cljs`
  - No longer needed since we're not calculating input positions
  - If needed elsewhere, import from `nodes.cljs` (resolve circular dependency if it exists)

- [ ] **4.2** Remove constants duplication
  - `default-node-width` and `default-node-height` are duplicated in `inputs.cljs`
  - Remove if not needed, or import from `nodes.cljs`

### Phase 5: Update Documentation

#### Changes Needed
- [ ] **5.1** Update function documentation
  - Remove position parameter from `create-input-helper` docs
  - Add examples showing inline usage

- [ ] **5.2** Update usage examples
  - Show inputs rendered inline with handles
  - Show custom layouts with flexbox/grid
  - Show absolute positioning if developer wants it

## Implementation Details

### New `create-input-helper` Signature

```clojure
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
    ;; Determine input mode, get values, etc.
    ;; Return hiccup for input component or connection info
    ;; NO position wrapper, NO absolute positioning
    ))
```

### New Input Component Return Value

```clojure
;; Returns just the component, no wrapper:
(case input-mode
  :connected
  (connection-info-component ...)
  :direct
  (input-component ...))
```

### New `create-handle-helper` Signature

```clojure
(defn create-handle-helper
  "Return a function node bodies can call to render handles inline.
   Signature:
     (render-handle-helper handle-id handle-type & [{:keys [class style label icon]}])
   - No :position parameter
   - Adds core classes/events automatically
   - Caller can extend styling via opts (merged into :class / :style)
   - Returns hiccup that participates in normal layout
   - Handles tooltip rendering: gets tooltip content from node, positions it relative to handle"
  [node connection edges tooltip-state]
  ;; implementation plan: build small button/div with data attributes,
  ;; add :on {:mousedown ...} etc., merge optional styling, no absolute coords
  ;; If tooltip-state matches this handle, render tooltip positioned relative to handle
  )
```

### New Tooltip Architecture

**Principle**: Node determines **what** the tooltip looks like, handle determines **where** it's positioned.

```clojure
;; render-tooltip returns hiccup with NO position information (just content)
(defn render-tooltip
  "Render tooltip content for a handle.
   Returns hiccup with tooltip content, no positioning.
   
   Args:
   - tooltip-state: Map with :node-id, :handle-id, :handle-type
   - node: The node that owns this tooltip
   
   Returns: Hiccup for tooltip content (no position wrapper)"
  [tooltip-state node]
  ;; Returns just the tooltip content, no absolute positioning
  )

;; Handle positions the tooltip relative to itself
;; In create-handle-helper, when rendering handle:
(when (and tooltip-state 
           (= (:node-id tooltip-state) node-id)
           (= (:handle-id tooltip-state) handle-id))
  [:div.tooltip-wrapper
   {:style {:position "absolute"
            :left (str tooltip-offset-x "px")
            :top (str tooltip-offset-y "px")
            :transform "translateX(-50%)"}}
   (tooltips/render-tooltip tooltip-state node)])
```

### Example: Custom Node Body with Inline Inputs

```clojure
(defn my-custom-node-body [node _connection render-handle-helper render-input-helper]
  [:div.node-body
   ;; Input with label, inline layout
   [:div.flex.items-center.gap-2.mb-2
    [:span.text-sm "Name:"]
    (render-input-helper "name-input")
    (render-handle-helper "name-input" :target)]
   
   ;; Another input with different layout
   [:div.flex.flex-col.gap-1.mb-2
    [:label.text-xs "Description"]
    [:div.relative
     (render-input-helper "desc-input")
     (render-handle-helper "desc-input" :target)]]
   
   ;; Output handle
   [:div.flex.justify-end
    (render-handle-helper "output" :source)]])
```

### Example: Using Absolute Positioning (If Needed)

```clojure
(defn my-node-body [node _connection render-handle-helper render-input-helper]
  [:div.node-body {:style {:position "relative"}}
   ;; Developer can still use absolute positioning if they want
   [:div {:style {:position "absolute" :left "10px" :top "20px"}}
    (render-input-helper "input1")]
   [:div {:style {:position "absolute" :right "10px" :top "20px"}}
    (render-handle-helper "input1" :target)]])
```

## Migration Guide

### For Existing Custom Node Bodies

**Before:**
```clojure
(defn my-node-body [node _connection render-handle-helper render-input-helper]
  [:div
   [:div "Content"]
   ;; Input automatically positioned
   (render-input-helper "input1" {:x "0%" :y "50%"})])
```

**After:**
```clojure
(defn my-node-body [node _connection render-handle-helper render-input-helper]
   [:div
    [:div "Content"]
    ;; Input + handle positioned inline by caller
    [:div.flex.items-center.gap-2
     (render-input-helper "input1")
     (render-handle-helper "input1" :target)]])
```

### For Default Node Body

The default node body will be updated to show the new pattern, so existing nodes using the default will automatically get the new layout.

## Benefits

1. **Simpler code**: Remove ~50 lines of position calculation logic
2. **More flexible**: Developers can use any CSS layout technique
3. **Better UX**: Inputs can be positioned next to labels, in forms, etc.
4. **Consistent**: Inputs work the same way as handles (developer-controlled)
5. **No duplication**: Remove duplicated position calculation code

## Open Questions

1. **Handle geometry for edges**: Once callers control layout, how do we compute absolute coordinates for edges?
   - **Plan**: Introduce a measurement step (e.g., `requestAnimationFrame` + `getBoundingClientRect`) that reads `.handle` elements, stores offsets in state, and reuses them when rendering edges.

2. **Tooltip positioning**: How should tooltips be positioned?
   - **Answer**: Node determines what the tooltip looks like (content), not where it's positioned.
   - `render-tooltip` should return hiccup with no position information (just content).
   - The tooltip hiccup should be passed into the handle.
   - The handle should place the tooltip near itself using `tooltip-offset-x` and `tooltip-offset-y`.
   - This way, the handle (which knows its own position) is responsible for positioning the tooltip relative to itself.

3. **Backward compatibility**: Should we maintain the position parameter for a transition period?
   - **Recommendation**: Remove it - breaking change is acceptable for simplification, and migration is straightforward

4. **Default node body**: Should it use inline or absolute positioning?
   - **Recommendation**: Use inline (flexbox) to demonstrate the new pattern

## Testing Plan

- [ ] Test default node body with new inline layout
- [ ] Test custom node bodies with inline inputs
- [ ] Test custom node bodies with absolute positioning (if dev wants it)
- [ ] Test input mode switching (connected vs direct)
- [ ] Test connection/disconnection
- [ ] Test handle positioning still works
- [ ] Test tooltips still work with new layout

