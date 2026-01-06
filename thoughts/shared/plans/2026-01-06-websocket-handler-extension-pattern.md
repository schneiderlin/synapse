# WebSocket Handler Extension Pattern Implementation Plan

## Overview

Reorganize WebSocket handlers in both backend (jetty-main) and frontend (replicant-main) to follow the same extension pattern as HTTP query/command handlers and frontend actions. Components will register their WebSocket event handlers through a registry pattern, allowing extensible event handling without modifying core handler code.

## Current State Analysis

### Backend (jetty-main):
- `make-ws-handler` in `core.clj:85-100` accepts `query-handler` and `command-handler`
- Event routing uses hardcoded `case` statement (`:chsk/ws-ping`, `:test/echo`, `:data/query`, `:data/command`)
- `ws_server.clj` has alternative pattern but still hardcoded

### Frontend (replicant-main):
- `ws-client.cljs` has basic WebSocket client with minimal handler
- `agent-web/main.cljs:15-78` shows complex handler with hardcoded events (`:agent/emit-text`, `:agent/partial-result`, etc.)

### Existing Extension Patterns:
- **HTTP query/command**: Components compose handlers with `or`, return `nil` if not handled
- **Frontend actions**: `make-execute-f` accepts extension functions, tries them first via `some`

## Desired End State

### Backend:
1. `make-ws-handler` accepts `& extension-fns` parameter like `make-execute-f`
2. Each component exposes a `ws-event-handler` function via its `interface.clj`
3. Components declare which event IDs they handle
4. Extension functions are tried first, then fallback to built-in events (`:chsk/ws-ping`, `:test/echo`, `:data/query`, `:data/command`)
5. Components can send any events via `chsk-send!` (no restriction)

### Frontend:
1. `make-ws-handler` accepts `& extension-fns` parameter
2. Extension functions pattern matches backend
3. Components expose their WebSocket handlers
4. Base composes component handlers via `replicant/ws-event-handlers` config key

### API Pattern:
```clojure
;; Backend component handler signature
(defn ws-event-handler [system event-msg]
  "Returns nil if event not handled, non-nil if handled"
  (let [{:keys [id ?data ?reply-fn]} event-msg]
    (case id
      :my-component/event-a (...handle event...)
      :my-component/event-b (...handle event...)
      nil)))  ; Return nil if not handled

;; Frontend component handler signature
(defn ws-event-handler [system event-msg]
  "Returns nil if event not handled"
  (let [{:keys [id ?data]} event-msg]
    (case id
      :my-component/event-a (...handle event...)
      nil)))
```

## What We're NOT Doing

1. NOT creating a separate WebSocket server - using existing Sente setup
2. NOT restricting what events components can send - components can send anything
3. NOT breaking existing `:data/query` and `:data/command` events
4. NOT changing the core Sente library usage

## Implementation Approach

Follow the established patterns:
- Backend: Match HTTP query/command handler composition pattern
- Frontend: Match `make-execute-f` extension pattern
- Use `or`/`some` for trying handlers in sequence
- Return `nil` for unhandled events

## Phase 1: Backend - New make-ws-handler with Extension Functions

### Overview
Create a new `make-ws-handler` that accepts extension functions, matching the pattern from `make-execute-f`.

### Changes Required:

#### 1. `components/jetty-main/src/com/zihao/jetty_main/core.clj`
**Changes**: Add new `make-ws-handler-with-extensions` function and update interface

```clojure
;; New function after line 100
(defn make-ws-handler-with-extensions
  "Creates a WebSocket event handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [system event-msg] and return non-nil if handled."
  [& extension-fns]
  (fn [stop-ch {:keys [ch-chsk chsk-send! connected-uids] :as ws-server}]
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg]
            (try
              ;; Try extension functions first
              (or (some #(when-let [result (% {:chsk-send! chsk-send!
                                               :connected-uids connected-uids
                                               :ws-server ws-server}
                                              event-msg)]
                           result)
                       extension-fns)
                  ;; Fallback to built-in events
                  (case id
                    :chsk/ws-ping nil
                    :test/echo (?reply-fn [id ?data])
                    nil))
              (catch Exception e
                (u/log ::error :exception e))))
          (recur))))))

;; For backwards compatibility, keep old function but deprecate
(defn make-ws-handler [query-handler command-handler]
  (fn [stop-ch {:keys [ch-chsk] :as ws-server}]
    ...)) ; existing implementation
```

#### 2. `components/jetty-main/src/com/zihao/jetty_main/interface.clj`
**Changes**: Expose the new function

```clojure
(defn make-ws-handler-with-extensions
  "Creates a WebSocket handler with extension functions"
  [& extension-fns]
  (core/make-ws-handler-with-extensions extension-fns))
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `clojure-eval (require 'com.zihao.jetty-main.core :reload)`
- [ ] New function is accessible via interface: `clojure-eval (com.zihao.jetty-main.interface/make-ws-handler-with-extensions)`

#### Manual Verification:
- Review the code follows the same pattern as `make-execute-f`
- Verify extension functions are called before built-in handlers

---

## Phase 2: Component Backend WebSocket Handlers

### Overview
Add `ws-event-handler` functions to component interfaces, starting with a simple example component (xiangqi).

### Changes Required:

#### 1. `components/xiangqi/src/com/zihao/xiangqi/api.clj`
**Changes**: Add WebSocket event handler

```clojure
;; Add after existing command-handler
(defn ws-event-handler
  "WebSocket event handler for xiangqi component"
  [system {:keys [id ?data ?reply-fn]}]
  (case id
    :xiangqi/move (let [{:keys [from to]} ?data]
                   (when ?reply-fn
                     (?reply-fn {:success? true
                                :new-state (some-function)})))
    nil))
```

#### 2. `components/xiangqi/src/com/zihao/xiangqi/interface.cljc`
**Changes**: Expose the handler

```clojure
;; Add after command-handler
(defn ws-event-handler
  "WebSocket event handler for xiangqi component"
  [system event-msg]
  #?(:clj (api/ws-event-handler system event-msg)
     :cljs (throw (ex-info "ws-event-handler only available in Clojure" {}))))
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `clojure-eval (require 'com.zihao.xiangqi.interface :reload)`
- [ ] Function exists: `clojure-eval (com.zihao.xiangqi.interface/ws-event-handler)`

#### Manual Verification:
- Review handler returns `nil` for unhandled events
- Review handler uses proper event structure

---

## Phase 3: Base Backend - Register Component Handlers

### Overview
Update `web-app/api.clj` to compose component WebSocket handlers like it does for query/command handlers.

### Changes Required:

#### 1. `bases/web-app/src/com/zihao/web_app/api.clj`
**Changes**: Add `ws-event-handler` composition and update Integrant config

```clojure
;; Add after existing handlers
(defn ws-event-handler [system event-msg]
  (or (language-learn/ws-event-handler system event-msg)
      (xiangqi/ws-event-handler system event-msg)))

;; Update config to include ws-handler
(def config
  {:ws/ws-server true   ; Set to false to disable WebSocket
   :ws/ws-handler {:ws-server (ig/ref :ws/ws-server)
                   :ws-event-handler (ig/ref :ws/event-handler)}
   :ws/event-handler []
   :jetty/routes {:ws-server (ig/ref :ws/ws-server)}
   :jetty/handler (ig/ref :jetty/routes)
   :adapter/jetty {:port (Integer. (or (System/getenv "PORT") "3000"))
                   :handler (ig/ref :jetty/handler)}
   :cljpy/python-env {:built-in "builtins"}})

;; Add init-key for ws-server
(defmethod ig/init-key :ws/ws-server [_ enabled?]
  (when enabled?
    (ws-server/make-ws-server)))

;; Add init-key for ws-handler using extension pattern
(defmethod ig/init-key :ws/ws-handler [_ {:keys [ws-server ws-event-handler]}]
  (when ws-server
    (let [stop-ch (async/chan)
          handler (apply jm/make-ws-handler-with-extensions ws-event-handler)]
      (handler stop-ch ws-server)
      stop-ch)))

(defmethod ig/halt-key! :ws/ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

;; Add halt-key for ws-server
(defmethod ig/halt-key! :ws/ws-server [_ ws-server]
  ;; Sente cleanup if needed
  nil)

;; Update :jetty/routes to use ws-server
(defmethod ig/init-key :jetty/routes [_ {:keys [ws-server] :as system}]
  (jm/make-routes system ws-server query-handler command-handler))
```

### Success Criteria:

#### Automated Verification:
- [ ] Code compiles: `clojure-eval (require 'com.zihao.web-app.api :reload)`
- [ ] System initializes: `clojure-eval (integrant.core/init com.zihao.web-app.api/config)`

#### Manual Verification:
- WebSocket server starts without errors
- Existing `:data/query` and `:data/command` still work
- New component WebSocket events can be handled

---

## Phase 4: Frontend - New make-ws-handler with Extension Functions

### Overview
Create `make-ws-handler-with-extensions` in replicant-main following the `make-execute-f` pattern.

### Changes Required:

#### 1. `components/replicant-main/src/com/zihao/replicant_main/replicant/ws_client.cljs`
**Changes**: Add extension-based handler maker

```clojure
(defn make-ws-handler-with-extensions
  "Creates a WebSocket event handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [system event-msg] and return non-nil if handled."
  [& extension-fns]
  (fn [stop-ch {:keys [ch-chsk chsk-send!] :as ws-client}]
    (let [store (get-in ws-client [:system :replicant/store])]
      (async/go-loop []
        (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
          (when (= port ch-chsk)
            (let [{:keys [?data id]} event-msg]
              (try
                ;; Try extension functions first
                (or (some #(when-let [result (% ws-client event-msg)]
                             result)
                         extension-fns)
                    ;; Fallback to built-in events
                    (case id
                      :chsk/ws-ping nil
                      nil))
                (catch :default e
                  (js/console.error "WebSocket handler error:" e)))
              (recur))))))))
```

#### 2. `components/replicant-main/src/com/zihao/replicant_main/interface.cljc`
**Changes**: Expose the new function (ClojureScript only)

```clojure
(defn make-ws-handler-with-extensions
  "Creates a WebSocket handler with extension functions (cljs only)"
  [& extension-fns]
  #?(:cljs (apply ws-client/make-ws-handler-with-extensions extension-fns)))
```

### Success Criteria:

#### Automated Verification:
- [ ] ClojureScript compiles: Check browser console for errors

#### Manual Verification:
- Code pattern matches `make-execute-f`
- Extension functions are called before built-in handlers

---

## Phase 5: Frontend Component WebSocket Handlers

### Overview
Add `ws-event-handler` to component interfaces (xiangqi as example).

### Changes Required:

#### 1. `components/xiangqi/src/com/zihao/xiangqi/interface.cljc`
**Changes**: Add frontend WebSocket event handler

```clojure
;; Add to interface (after backend ws-event-handler)
(defn ws-event-handler-frontend
  "Frontend WebSocket event handler for xiangqi component"
  [system event-msg]
  #?(:cljs
     (let [{:keys [id ?data]} event-msg]
       (case id
         :xiangqi/game-state-update
         (let [store (:replicant/store system)]
           (swap! store assoc :xiangqi/state ?data)
           true)  ; Return truthy to indicate handled
         nil))
     :clj nil))
```

### Success Criteria:

#### Automated Verification:
- ClojureScript compiles without errors

#### Manual Verification:
- Handler returns `nil` for unhandled events
- Handler updates store appropriately

---

## Phase 6: Base Frontend - Register Component Handlers

### Overview
Update `web-app/main.cljs` to use the new extension-based WebSocket handler.

### Changes Required:

#### 1. `bases/web-app/src/com/zihao/web_app/main.cljs`
**Changes**: Add ws-event-handlers composition

```clojure
;; Import component handlers
(:require
 ...
 [com.zihao.xiangqi.interface :as xiangqi])

;; Add to config
(def config
  {:replicant/el "app"
   :replicant/store {:msgs [] :streaming-text "" :tool-call-active? false}
   :replicant/routes router/routes
   :replicant/get-location-load-actions router/get-location-load-actions
   :replicant/execute-actions [replicant-component/execute-action
                               xiangqi-actions/execute-action
                               fsrs-actions/execute-action
                               fsrs-actions/execute-effect
                               lingq-actions/execute-action]
   :ws/ws-client true   ; Set to false to disable WebSocket
   :ws/ws-handler {:ws-client (ig/ref :ws/ws-client)
                   :ws-event-handlers (ig/ref :ws/event-handlers)}
   :ws/event-handlers [xiangqi/ws-event-handler-frontend]
   :replicant/render-loop {:store (ig/ref :replicant/store)
                           :el (ig/ref :replicant/el)
                           :routes (ig/ref :replicant/routes)
                           :interpolate (apply rm1/make-interpolate [])
                           :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                           :execute-actions (ig/ref :replicant/execute-actions)
                           :base-url ""}})

;; Update ws-handler init-key
(defmethod ig/init-key :ws/ws-handler [_ {:keys [ws-client ws-event-handlers]}]
  (when ws-client
    (let [stop-ch (async/chan)]
      ;; Will be started after system is initialized
      {:stop-ch stop-ch
       :ws-client ws-client
       :event-handlers ws-event-handlers})))

;; Update ws-client init-key to accept enabled? boolean
(defmethod ig/init-key :ws/ws-client [_ enabled?]
  (when enabled?
    (ws-client/make-ws-client)))

;; Update start-ws-handler function
(defn start-ws-handler [system]
  (when-let [{:keys [stop-ch ws-client event-handlers]} (:ws/ws-handler system)]
    (when (and ws-client stop-ch)
      (let [handler (apply rm1/make-ws-handler-with-extensions event-handlers)]
        (handler stop-ch ws-client)))))
```

### Success Criteria:

#### Automated Verification:
- ClojureScript compiles
- No console errors on page load

#### Manual Verification:
- WebSocket connection established
- Component events are received and handled
- Existing functionality not broken

---

## Testing Strategy

### Unit Tests:
- Test that extension functions are called in order
- Test that `nil` return causes fallback to next handler
- Test that built-in events still work

### Integration Tests:
- Test backend WebSocket with component handlers
- Test frontend WebSocket with component handlers
- Test bi-directional communication

### Manual Testing Steps:
1. Start backend server, verify WebSocket connects
2. Send a `:data/query` event, verify it still works
3. Send a component-specific event (e.g., `:xiangqi/move`), verify component handles it
4. From backend, send an event to frontend, verify frontend handler processes it
5. Add a new component handler, verify it integrates without modifying core code

## Performance Considerations

- Extension functions are tried via `some` - should be O(n) where n is number of handlers
- Each handler should quickly return `nil` for unhandled events (using `case`)
- No performance impact for existing built-in events

## Migration Notes

### Backwards Compatibility:
- Old `make-ws-handler` function kept for compatibility
- Existing `:data/query` and `:data/command` continue to work
- `agent-web` can be migrated incrementally

### Incremental Migration:
1. New bases can use extension pattern immediately
2. Existing bases can migrate incrementally:
   - First: Add new handler alongside old
   - Second: Move component handlers one at a time
   - Finally: Remove old handler

## References

- Original research: `thoughts/shared/research/2026-01-06-query-command-handler-patterns.md`
- Backend HTTP pattern: `components/jetty-main/src/com/zihao/jetty_main/core.clj:21-41`
- Frontend action pattern: `components/replicant-main/src/com/zihao/replicant_main/replicant/actions.cljs:78-107`
- Component composition: `bases/web-app/src/com/zihao/web_app/api.clj:10-16`
