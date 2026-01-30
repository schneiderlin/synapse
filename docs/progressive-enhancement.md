# Progressive Enhancement

## Overview

Progressive enhancement is a technique to gradually add interactivity to static HTML pages. This system allows you to mark specific HTML elements with ClojureScript components that will replace or enhance the static content when JavaScript loads.

The system uses:
- **Replicant**: A data-driven rendering library for ClojureScript (renders hiccup to DOM)
- **Integrant**: A dependency injection and system initialization library
- **Custom HTML attributes**: To mark elements for enhancement

## How It Works

### 1. System Initialization

The progressive enhancement system is initialized using Integrant's configuration:

```clojure
(def config
  {:replicant/store {}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action xiangqi-actions/execute-action]
   :replicant/render-loop {:store (ig/ref :replicant/store)
                           :routes (ig/ref :replicant/routes)
                           :interpolate (apply rm1/make-interpolate [])
                           :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                           :execute-actions (ig/ref :replicant/execute-actions)
                           :base-url "http://localhost:3000"}})

(defmethod ig/init-key :replicant/store [_ init-value]
  (atom init-value))
```

The system creates:
- A global state atom for application state
- Route handlers for navigation
- Action execution functions
- A render loop that finds and enhances marked elements

### 2. Element Discovery and Enhancement

The render loop finds all elements with the `x-data-replicant-type` attribute:

```clojure
(defmethod ig/init-key :replicant/render-loop [_ {:keys [store]}]
  (let [elements (array-seq (.querySelectorAll js/document "[x-data-replicant-type]"))]
    (doseq [[idx el] (map-indexed vector elements)]
      (let [type-str (.getAttribute el "x-data-replicant-type")
            type (keyword type-str)
            initial-state (get-initial-state el)
            element-store (atom initial-state)
            render-fn (get-render-fn type)]
        ;; Set up watch for this element
        (add-watch
         element-store ::render
         (fn [_ _ _ state]
           (r/render el (render-fn state))))
        ;; Store reference in main store
        (swap! store assoc-in [:elements (or (.-id el) (str "element-" idx)) :store] element-store)
        ;; Initial render
        (r/render el (render-fn initial-state)))))
  (swap! store assoc :app/started-at (js/Date.)))
```

For each element found:
1. Extract the component type from `x-data-replicant-type` attribute
2. Read initial state from `x-data-replicant-initial-state` attribute (EDN format)
3. Create an isolated atom for the element's state
4. Register a watch that re-renders when state changes
5. Store the element's atom in the global store
6. Perform the initial render

## Minimal Example

### HTML File (index.html)

```html
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Progressive Enhancement Demo</title>
    <link href="./output.css" rel="stylesheet">
    <script src="./js/main.js" defer></script>
</head>
<body>
    <!-- Static content that will be enhanced -->
    <div id="counter-1"
         x-data-replicant-type="counter"
         x-data-replicant-initial-state='{:count 0}'>
        <p>Counter: 0</p>
    </div>

    <div id="counter-2"
         x-data-replicant-type="counter"
         x-data-replicant-initial-state='{:count 10}'>
        <p>Counter: 10</p>
    </div>
</body>
</html>
```

### ClojureScript Component (progressive_enhancement.cljs)

```clojure
(ns com.zihao.web-app.progressive-enhancement
  (:require
   [clojure.edn :as edn]
   [replicant.dom :as r]
   [integrant.core :as ig]))

;; Render function for counter component
(defn render-counter [state]
  (let [count (:count state)]
    [:div {:class ["card" "bg-base-100" "shadow-xl"]}
     [:div {:class ["card-body"]}
      [:h2 {:class ["card-title"]} "Counter Component"]
      [:p {:class ["text-3xl" "font-bold"]} count]
      [:div {:class ["card-actions" "justify-end"]}
       [:button {:class ["btn" "btn-secondary"]
                 :on {:click [[:store/update-in [:count] inc] [[:debug/print "Increment"]]]}}
        "Increment"]
       [:button {:class ["btn" "btn-primary"]
                 :on {:click [[:store/update-in [:count] dec]]}}
        "Decrement"]]]]))

;; Get render function by type
(defn get-render-fn [type]
  (case type
    "counter" render-counter
    :counter render-counter
    (throw (ex-info "Unknown replicant type" {:type type}))))

;; Get initial state from element attribute
(defn get-initial-state [el]
  (let [initial-state-str (.getAttribute el "x-data-replicant-initial-state")]
    (if initial-state-str
      (edn/read-string initial-state-str)
      {})))

;; Integrant initialization for store
(defmethod ig/init-key :replicant/store [_ init-value]
  (atom init-value))

;; Integrant initialization for render loop
(defmethod ig/init-key :replicant/render-loop [_ {:keys [store]}]
  (let [elements (array-seq (.querySelectorAll js/document "[x-data-replicant-type]"))]
    (doseq [[idx el] (map-indexed vector elements)]
      (let [type-str (.getAttribute el "x-data-replicant-type")
            type (keyword type-str)
            initial-state (get-initial-state el)
            element-store (atom initial-state)
            render-fn (get-render-fn type)]
        ;; Set up watch to re-render on state change
        (add-watch
         element-store ::render
         (fn [_ _ _ state]
           (r/render el (render-fn state))))
        ;; Store element reference in main store
        (swap! store assoc-in [:elements (or (.-id el) (str "element-" idx)) :store] element-store)
        ;; Initial render
        (r/render el (render-fn initial-state)))))
  (swap! store assoc :app/started-at (js/Date.)))

;; System configuration
(def config
  {:replicant/store {}
   :replicant/render-loop {:store (ig/ref :replicant/store)}})

(defonce !system (atom nil))

;; Main function to start the system
(defn ^:dev/after-load main []
  (let [system (ig/init config)
        store (:replicant/store system)]
    (println "Progressive enhancement started")
    (reset! !system system)))
```

## Key Concepts

### State Isolation

Each enhanced element has its own isolated state atom, stored at `[:elements element-id :store]` in the global store. This allows multiple instances of the same component to have independent state.

### Reactive Rendering

When an element's state changes, the watch triggers a re-render using Replicant's efficient virtual DOM diffing:

```clojure
(add-watch element-store ::render
  (fn [_ _ _ state]
    (r/render el (render-fn state))))
```

### Data-Driven Events

Event handlers are data structures rather than functions, enabling:
- Declarative event handling
- Easy debugging (events can be printed and inspected)
- Event interpolation for dynamic values
- Composable actions

```clojure
[:on {:click [[:store/update-in [:count] inc]
              [:debug/print "Increment"]]}]
```

### Action Chaining

Actions can chain additional actions:

```clojure
[:on {:click [[:debug/print "Going back"]
              [:xiangqi/go-back]]}]
```

### Initial State

Initial state is serialized as EDN in the HTML attribute, enabling server-side rendering or static HTML with client-side enhancement:

```html
<div x-data-replicant-initial-state='{:count 5 :name "Widget"}'>
```

## Files

- `bases/web-app/src/com/zihao/web_app/progressive_enhancement.cljs` - Main progressive enhancement implementation
- `components/replicant-main/src/com/zihao/replicant_main/replicant/actions.cljs` - Action execution system
- `components/xiangqi/src/com/zihao/xiangqi/actions.cljc` - Example action handlers
- `components/xiangqi/src/com/zihao/xiangqi/render.cljc` - Example component rendering

## External Dependencies

- [Replicant](https://replicant.fun/) - Data-driven rendering library
- [Integrant](https://github.com/weavejester/integrant) - Dependency injection framework
- [domkm.silk](https://github.com/domkm/silk) - URL routing
