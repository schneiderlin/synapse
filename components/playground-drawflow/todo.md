# Plan: ReactFlow-like Workflow Canvas with Vanilla JS (shadow-cljs) + Hiccup + Replicant

## Overview
Build a node-based workflow canvas editor similar to ReactFlow, using:
- **Vanilla JavaScript** (compiled from ClojureScript via shadow-cljs)
- **HTML/Hiccup** for declarative UI structure
- **Replicant** for reactive DOM rendering and state management
- **HTML div elements** for nodes (like ReactFlow) with absolute positioning
- **SVG overlay** for rendering edges (connections between nodes)

## Architecture

### State Management
- Store workflow state in Replicant store (atom):
  - `:nodes` - **map keyed by node-id** `{node-id {:id, :type, :position {:x, :y}, :data, :selected?}}`
  - `:edges` - vector of edge maps `{:id, :source, :target, :sourceHandle, :targetHandle, :type}`
  - `:viewport` - `{:x, :y, :zoom}` for pan/zoom state
  - `:connection` - temporary connection state when dragging from handle
  - `:dragging` - current drag state for nodes/pan

### Rendering Strategy
- Use a div container as the main canvas with relative positioning
- Render nodes as HTML div elements with absolute positioning
- Apply CSS transforms (translate/scale) for viewport pan/zoom
- Use SVG overlay (positioned absolutely) for rendering edges as paths
- Nodes are regular HTML divs, making them easy to style with CSS/TailwindCSS
- Use Hiccup to generate HTML structure declaratively
- Leverage Replicant's `r/render` to update DOM reactively when store changes

## Stage 1: Core Functionality ✅ COMPLETED
- ✅ Node rendering, selection, dragging
- ✅ Edge rendering, creation, selection, deletion
- ✅ Connection handles (source/target ports)
- ✅ Viewport pan & zoom
- ✅ Coordinate system and transforms

## Stage 2: Reusable Node Components ✅ COMPLETED

### Goals
- ✅ Make `render-node` accept a replicant component as the node body
- ✅ Move handle rendering inside the body component
- ✅ Allow developers to create node bodies with 0, 1, or more handles
- ✅ Allow specifying handle type (source/target) and position

### Implementation Plan
- [x] **2.1** Refactor `render-node` to accept `:body-component` function ✅
  - ✅ Body component receives: `node`, `connection`, `render-handle-helper`
  - ✅ Body component returns hiccup for node body content
  - ✅ Body component can call `render-handle-helper` to render handles

- [x] **2.2** Create handle rendering helper for body components ✅
  - ✅ Helper function that body components can call (`create-handle-helper`)
  - ✅ Takes handle-id, handle-type, and optional position
  - ✅ Returns hiccup for handle element
  - ✅ Handles event wiring automatically

- [x] **2.3** Update default node body to use new pattern ✅
  - ✅ Migrated existing default node body to new component pattern (`default-node-body`)
  - ✅ Demonstrates handle usage in body component

- [x] **2.4** Update node data structure ✅
  - ✅ Body component stored in node `:data :body-component`
  - ✅ Handle definitions can be specified by body component
  - ✅ Handle positions can be stored in `:data :handle-data` or passed inline to helper

### Completion Notes

#### What Was Implemented
1. **`create-handle-helper` function**: Creates a helper function that body components can use to render handles
   - Supports custom positions (pixels or percentages like `"100%"`)
   - Automatically wires up event handlers
   - Returns hiccup for handle elements

2. **`default-node-body` component**: Default body component that demonstrates the pattern
   - Maintains backward compatibility
   - Uses handle helper to render handles
   - Supports handle lists from `:data :handles`

3. **Refactored `render-node`**: Now accepts `:body-component` from node data
   - Falls back to `default-node-body` if not provided
   - Creates handle helper and passes it to body component
   - Body component has full control over handle rendering

4. **Enhanced handle positioning**: 
   - `get-handle-position` and `get-handle-position-relative` support custom positions
   - Positions can be stored in `:data :handle-data` as `{"handle-id" {:position {:x "100%" :y "50%"}}}`
   - Supports both percentage strings (`"100%"`) and pixel values (`150`)

#### Usage Examples

**Default node body** (automatic):
```clojure
{:id "node-1"
 :data {:label "Node 1"
        :content "Uses default body"}}
```

**Custom node body**:
```clojure
{:id "node-2"
 :data {:label "Custom Node"
        :body-component my-custom-body-component}}
```

**Custom body with handles**:
```clojure
(defn my-node-body [node _connection render-handle-helper]
  [:<>
   [:div "Custom content"]
   (render-handle-helper "output1" :source {:x "100%" :y "50%"})
   (render-handle-helper "input1" :target {:x "0%" :y "50%"})])
```

**Default body with custom handle positions**:
```clojure
{:id "node-3"
 :data {:handles {:source ["output1" "output2"]
                 :target ["input1"]}
        :handle-data {"output1" {:position {:x "100%" :y "30%"}}
                     "output2" {:position {:x "100%" :y "70%"}}
                     "input1" {:position {:x "0%" :y "50%"}}}}}
```

### Technical Details

#### Node Structure (New)
```clojure
[:div.node {:id node-id
            :style {:position "absolute" :left x :top y}
            :on {:mousedown [[:canvas/node-mouse-down id :event/event]]}}
 ;; Body component renders here
 (body-component node connection render-handle-helper)]
```

#### Body Component Signature
```clojure
(defn my-node-body [node connection render-handle-helper]
  [:div.node-body
   ;; Custom content
   [:div "My node content"]
   ;; Render handles as needed
   (render-handle-helper "output1" :source {:x "100%" :y "50%"})
   (render-handle-helper "input1" :target {:x "0%" :y "50%"})])
```

#### Handle Helper Function
```clojure
;; Created by create-handle-helper, called by body components:
(render-handle-helper handle-id handle-type & [position])
;; Returns hiccup for handle element
;; Handles event wiring automatically
;; Position can be absolute pixels or percentages relative to node
;; Example: (render-handle-helper "output1" :source {:x "100%" :y "50%"})
```

### Event Handling Pattern
- Use Replicant's **data-driven event system** (not functions!)
- Attach event handlers via Hiccup `:on` map with **pure data** (action vectors):
  ```clojure
  :on {:mousedown [[:canvas/node-mouse-down node-id :event/event]]
       :mousemove [[:canvas/mouse-move :event/event]]}
  ```
- Use placeholders like `:event/event` which get interpolated to the actual event object
- Action handlers extract values from interpolated event (e.g., `(.-clientX event)`)
- Store changes trigger re-render via Replicant watch
- **Document-level listeners**: For dragging, we set up document-level listeners programmatically in `events.cljs` since we can't use `:on` on `js/document`

### Coordinate System
- HTML/CSS uses pixel-based coordinate system
- Need to track:
  - Canvas coordinates (logical positions of nodes in canvas space)
  - Screen coordinates (mouse position relative to viewport)
  - Viewport transform (pan + zoom applied via CSS transform)
- Conversion functions:
  - `screen->canvas` - convert mouse position to canvas position (account for transform)
  - `canvas->screen` - convert canvas position to screen position
  - Use `getBoundingClientRect()` and transform matrix for accurate conversions

## Stage 3: Handle Tooltips & Node Input Handling

### Goals
- ✅ Add tooltip support for handles (custom replicant components)
- Enable two input modes for target handles:
  1. **Connected input**: Value comes from connected source node
  2. **Direct input**: User can type value directly into the handle
- Design intuitive UI/UX for switching between input modes

### Feature 1: Handle Tooltips ✅ COMPLETED

#### Design
- **Trigger**: Hover over handle (with small delay ~300ms to avoid flickering) ✅
- **Position**: Tooltip appears below handle with configurable offset (`tooltip-offset-x`, `tooltip-offset-y`) ✅
  - Centered horizontally on handle using `translateX(-50%)`
  - Positioned relative to node (canvas coordinates), so tooltips scale/pan with viewport ✅
- **Component**: Custom replicant component (fully customizable) ✅
- **Content**: Can display any information (handle name, type, value, description, etc.) ✅
- **Styling**: 
  - Small arrow pointing up to handle ✅
  - TailwindCSS/DaisyUI styling with theme support ✅
  - Shadow for depth ✅
  - Fade-in animation with scale effect ✅

#### UI Mockup
```
[Node] ──●── [Connected Node]
         ↑
    [Tooltip]
    ┌─────────────┐
    │ Handle: out │
    │ Type: string│
    │ Value: "..."│
    └─────────────┘
```

#### Implementation Plan
- [x] **3.1** Add tooltip state to store ✅
  - Track which handle is currently showing tooltip: `{:tooltip {:node-id, :handle-id, :handle-type}}`
  - Track tooltip position for positioning calculations

- [x] **3.2** Create tooltip rendering system ✅
  - Tooltip component receives: `node`, `handle-id`, `handle-type`, `handle-data`
  - Tooltip component can be specified per handle in `:data :handle-data {"handle-id" {:tooltip-component ...}}`
  - Default tooltip component if not specified
  - Position tooltip relative to handle (account for viewport transform)

- [x] **3.3** Add hover event handlers ✅
  - Mouse enter on handle → show tooltip (with delay)
  - Mouse leave handle/tooltip → hide tooltip
  - Mouse move → update tooltip position if needed

- [x] **3.4** Tooltip positioning logic ✅
  - Calculate handle position in screen coordinates
  - Position tooltip above/below handle
  - Check viewport bounds, adjust if needed
  - Account for viewport pan/zoom

### Feature 2: Node Input Handling

#### Design Overview
Target handles (inputs) can receive values in two ways:
1. **Connected Mode**: Edge connects to another node's output
2. **Direct Input Mode**: User types value directly

#### UI Design: Inline Input Field

- When handle is **not connected**: Show small input field next to handle
- When handle **is connected**: Show connected node info with disconnect button
- Input field appears inline in node body, positioned near the handle
- Visual indicator: Different handle style for connected vs. direct input

```
┌─────────────────────┐
│ Node Label          │
│                     │
│ Input: [____] ●     │  ← Direct input (not connected)
│                     │
│ Output: ●           │
└─────────────────────┘

┌─────────────────────┐
│ Node Label          │
│                     │
│ Input: [Node-1] ✕ ● │  ← Connected (shows source, disconnect button)
│                     │
│ Output: ●           │
└─────────────────────┘
```

**Visual States:**

1. **Empty/Unconnected Handle**:
   ```
   Input: [________] ●
   ```
   - Input field visible, empty
   - Handle visible on right
   - User can type directly

2. **Direct Input (Typed Value)**:
   ```
   Input: [Hello World] ●
   ```
   - Input field shows typed value
   - Handle visible
   - User can edit by clicking input field

3. **Connected Handle**:
   ```
   Input: [Node-1: output] ✕ ●
   ```
   - Shows source node ID and handle ID
   - Disconnect button (✕) to remove connection
   - Handle visible but styled differently (maybe blue border)
   - Click disconnect → switches to direct input mode

#### Implementation Plan

- [ ] **3.5** Extend handle data structure
  - Add `:input-mode` to handle data: `:connected` or `:direct`
  - Add `:direct-value` for typed values
  - Add `:input-component` for custom input rendering (optional)

- [ ] **3.6** Create input field component
  - Default input component for direct input
  - Renders inline near handle in node body
  - Supports different input types (text, number, etc.)
  - Custom input components can be specified per handle

- [ ] **3.7** Connection state management
  - When edge connects to target handle → set `:input-mode :connected`
  - Store connection info: `{:source-node-id, :source-handle-id}`
  - When edge disconnects → set `:input-mode :direct` (preserve direct value if exists)

- [ ] **3.8** UI rendering logic
  - Body component checks handle state
  - If `:input-mode :connected` → render connection info + disconnect button
  - If `:input-mode :direct` → render input field
  - Handle visual styling reflects state

- [ ] **3.9** Input field interactions
  - Click input field → focus and allow editing
  - Type in input field → update `:direct-value` in store
  - Disconnect button → remove edge, switch to direct input mode
  - Handle still works for creating new connections

- [ ] **3.10** Value resolution logic
  - When node needs input value:
    - If `:input-mode :connected` → get value from source node
    - If `:input-mode :direct` → use `:direct-value`
    - If both exist (hybrid mode) → prefer override if set, else connection

#### Data Structure

**Handle Data Structure**:
```clojure
{:data {:handle-data {"input1" {:tooltip-component my-tooltip-component
                                :input-mode :direct  ; or :connected
                                :direct-value "Hello"
                                :input-component my-input-component
                                :connection {:source-node-id "node-2"
                                            :source-handle-id "output1"}}}}
```

**Node State**:
```clojure
{:id "node-1"
 :data {:handle-data {"input1" {:input-mode :direct
                                :direct-value "typed value"
                                :tooltip-component default-tooltip}}}}
```

**After Connection**:
```clojure
{:id "node-1"
 :data {:handle-data {"input1" {:input-mode :connected
                                :connection {:source-node-id "node-2"
                                            :source-handle-id "output1"}}}}}
```

#### UI Components Needed

1. **Default Tooltip Component**:
   - Shows handle ID, type, current value/connection status
   - Can be overridden with custom component

2. **Default Input Component**:
   - Text input field
   - Can be overridden with custom component (e.g., number input, dropdown, etc.)

3. **Connection Info Component**:
   - Shows source node and handle
   - Disconnect button
   - Optional: value preview from source

4. **Handle Visual States**:
   - Normal: Gray border
   - Connected: Blue border, maybe filled
   - Direct input (with value): Green border or indicator
   - Hover: Highlighted

#### User Flow

1. **Creating Direct Input**:
   - User clicks on empty input field next to handle
   - Types value
   - Value stored in handle data

2. **Creating Connection**:
   - User drags from source handle
   - Drops on target handle
   - Connection created, input mode switches to `:connected`
   - Direct value preserved but not used

3. **Switching from Connection to Direct Input**:
   - User clicks disconnect button (✕)
   - Edge removed
   - Input mode switches to `:direct`
   - Previous direct value restored (if existed)

4. **Switching from Direct Input to Connection**:
   - User creates connection
   - Input mode switches to `:connected`
   - Direct value preserved but hidden

#### Edge Cases

- **Handle with both connection and direct value**: Use connection by default, show override option
- **Multiple edges to same handle**: Not supported initially (one connection per handle)
- **Deleting source node**: Disconnect target handle, switch to direct input
- **Tooltip positioning near viewport edge**: Auto-adjust position
- **Tooltip on zoomed canvas**: Account for zoom level in positioning

## File Structure
```
components/playground-drawflow/
├── src/com/dx/playground_drawflow/
│   ├── core.cljs                # Canvas component
│   ├── nodes.cljs               # Node rendering & logic
│   ├── edges.cljs               # Edge rendering & logic
│   ├── viewport.cljs            # Pan/zoom logic
│   ├── events.cljs              # Event handlers
│   ├── actions.cljs             # Action executors
│   ├── tooltips.cljs            # Tooltip rendering & positioning
│   └── inputs.cljs              # Input field components (NEW)
```

## Stage 3 Completion Notes

### Feature 1: Handle Tooltips ✅

#### What Was Implemented
1. **Tooltip state management**: Added tooltip state to store tracking `:node-id`, `:handle-id`, `:handle-type`, `:timeout-id`, and `:hovering-tooltip?`

2. **Tooltip rendering system**: 
   - Created `default-tooltip-component` with TailwindCSS/DaisyUI styling
   - Support for custom tooltip components per handle via `:data :handle-data {"handle-id" {:tooltip-component ...}}`
   - Tooltips are rendered as children of nodes (not canvas) for proper viewport transform support

3. **Hover event handlers**:
   - Mouse enter on handle → show tooltip with ~300ms delay
   - Mouse leave handle/tooltip → hide tooltip with small delay to allow moving to tooltip
   - Tooltip stays visible when hovering over it

4. **Tooltip positioning**:
   - Simple positioning: tooltip appears below handle with configurable offset (`tooltip-offset-x`, `tooltip-offset-y`)
   - Positioned relative to node (canvas coordinates), so tooltips scale/pan with viewport
   - Centered horizontally on handle using `translateX(-50%)`
   - Arrow points up to handle

#### Technical Details
- **Styling**: Uses TailwindCSS and DaisyUI for modern, themeable tooltips
- **Architecture**: Tooltips are children of nodes to avoid circular dependencies and support viewport transforms
- **Positioning**: Uses `get-handle-position-relative` to get handle position in node-relative coordinates
- **Animation**: CSS fade-in animation with scale effect
- **Dependencies**: Resolved circular dependency by passing handle position as parameter instead of importing nodes namespace
