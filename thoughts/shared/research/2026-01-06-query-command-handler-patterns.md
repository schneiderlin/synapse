---
date: 2026-01-06T09:28:00+08:00
researcher: zihao
git_commit: 5a2fba40db9dab9efef4411ecbb39e4511039398
branch: main
repository: synapse
topic: "How jetty-main and replicant-main handle query/command handlers and extension functions, for reference when organizing websocket handlers"
tags: [research, codebase, jetty-main, replicant-main, websocket, architecture]
status: complete
last_updated: 2026-01-06
last_updated_by: zihao
---

# Research: Query/Command Handler Patterns and Extension Function Architecture

**Date**: 2026-01-06T09:28:00+08:00
**Researcher**: zihao
**Git Commit**: 5a2fba40db9dab9efef4411ecbb39e4511039398
**Branch**: main
**Repository**: synapse

## Research Question

How do jetty-main and replicant-main components handle query/command handlers and accept extension functions from other modules? This research is to inform the reorganization of websocket handlers to follow similar patterns.

## Summary

The codebase uses a consistent pattern for extensibility across both backend (HTTP/WebSocket) and frontend (SPA):

1. **Backend (jetty-main)**: Uses `query-handler` and `command-handler` functions that accept a system map and a query/command map. Components register their handlers by providing functions that return `nil` if they don't handle the specific query/command.

2. **Frontend (replicant-main)**: Uses `make-interpolate` and `make-execute-f` functions that accept extension functions. Extension functions are tried first before falling back to built-in behavior.

3. **Current WebSocket handling**: WebSocket code exists but is not organized with the same extension pattern as HTTP query/command handlers.

## Detailed Findings

### Backend: jetty-main Query/Command Handler Pattern

#### HTTP Handler Creation (`components/jetty-main/src/com/zihao/jetty_main/core.clj:21-41`)

The HTTP handlers are created with `make-http-query-handler` and `make-http-command-handler`:

```clojure
(defn make-http-query-handler [system query-handler]
  (fn [request]
    (let [query (:body-params request)
          result (query-handler system query)]
      (response/response {:success? true
                          :result result}))))

(defn make-http-command-handler [system command-handler]
  (fn [request]
    (let [command (:body-params request)
          result (command-handler system command)]
      (response/response {:success? true
                          :result result}))))
```

#### Route Creation (`components/jetty-main/src/com/zihao/jetty_main/core.clj:66-83`)

The `make-routes` function accepts system, ws-server, query-handler, and command-handler:

```clojure
(defn make-routes [system ws-server query-handler command-handler]
  (let [common-routes [["/" {:get html-handler
                             :post html-handler}]
                       ["/api" {:middleware [...]}
                        ["/query" {:post (make-http-query-handler system query-handler)}]
                        ["/command" {:post (make-http-command-handler system command-handler)}]
                        ...]]]
    ...))
```

#### Component Handler Registration Pattern (`bases/web-app/src/com/zihao/web_app/api.clj:10-16`)

Components register their handlers by composing `or` expressions:

```clojure
(defn command-handler [system command]
  (or (language-learn/command-handler system command)
      (xiangqi/command-handler system command)))

(defn query-handler [system query]
  (or (language-learn/query-handler system query)
      (xiangqi/query-handler system query)))
```

Each component's handler returns `nil` if it doesn't handle the specific query/command:

```clojure
;; Example from xiangqi/api.clj:16-22
(defn command-handler [_system {:command/keys [kind data]}]
  (case kind
    :command/export-game-tree
    (let [{:keys [game-tree]} data]
      (spit (io/file "data/game_tree/game_tree.edn") (pr-str game-tree))
      nil)
    nil))  ; Return nil if not handled
```

#### Component Interface Pattern (`components/language-learn/src/com/zihao/language_learn/interface.clj:14-24`)

Components expose their handlers through their interface namespace:

```clojure
(defn execute-action
  "Execute action for language-learn component (fsrs + lingq)"
  [system event action args]
  (or (fsrs-actions/execute-action system event action args)
      (lingq-actions/execute-action system event action args)))

(defn query-handler
  "Query handler for language-learn component (fsrs + lingq)"
  [system query]
  (or (fsrs-api/query-handler system query)
      (lingq-api/query-handler system query)))

(defn command-handler
  "Command handler for language-learn component (fsrs + lingq)"
  [system command]
  (or (fsrs-api/command-handler system query)
      (lingq-api/command-handler system command)))
```

### Frontend: replicant-main SPA Extension Pattern

#### make-interpolate Extension Function (`components/replicant-main/src/com/zihao/replicant_main/replicant/utils.cljc:70-102`)

The `make-interpolate` function accepts extension functions that can handle custom placeholders:

```clojure
(defn make-interpolate
  "Creates an interpolation function for handling event data with optional extensions.

   Parameters:
   - extension-fns: Optional functions to extend the interpolation behavior.
                    Each function should take (event case-key) and return a value if handled, nil otherwise.

   Returns: An interpolation function that can be used to process event data"
  [& extension-fns]
  (fn [event data]
    (walk/postwalk
     (fn [x]
       (let [result (or
                     (some #(when-let [result (% event x)]
                              result)
                           extension-fns)
                     (case x
                       :event/target.value (.. event -target -value)
                       :event/target.int-value (parse-int (.. event -target -value))
                       ...
                       nil))]
         (if (some? result)
           result
           x)))
     data)))
```

Key pattern: Extension functions are tried first via `some`. If any extension returns a non-nil value, it's used. Otherwise, fall back to the `case` statement for built-in placeholders.

#### make-execute-f Extension Function (`components/replicant-main/src/com/zihao/replicant_main/replicant/actions.cljs:78-107`)

The `make-execute-f` function accepts extension functions for custom actions:

```clojure
(defn make-execute-f [& extension-fns]
  (letfn [(f [{:keys [store] :as system} e actions]
            (doseq [[action & args] actions]
              (let [result (or
                            (some #(when-let [result (% system e action args)]
                                     result)
                                  extension-fns)
                            (case action
                              :store/assoc (apply swap! store assoc args)
                              :store/assoc-in (apply swap! store assoc-in args)
                              ...
                              (throw (js/Error. (str "No matching action type found: " action)))))]
                (when (vector? result)
                  (f system e result)))))]
    f))
```

Same pattern: Extension functions are tried first via `some`. If none handle it, fall back to built-in actions.

#### SPA Setup with Extensions (`bases/web-app/src/com/zihao/web_app/main.cljs:38-55`)

The SPA system is configured with extension functions:

```clojure
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
   :replicant/render-loop {:system {:store (ig/ref :replicant/store)
                                    :el (ig/ref :replicant/el)
                                    :routes (ig/ref :replicant/routes)
                                    :interpolate (apply rm1/make-interpolate [])
                                    :get-location-load-actions (ig/ref :replicant/get-location-load-actions)
                                    :execute-actions (ig/ref :replicant/execute-actions)
                                    :base-url ""}
                           :hash-router? true}})

(defmethod ig/init-key :replicant/execute-actions [_ extension-fns]
  (apply rm1/make-execute-f extension-fns))
```

#### Component Extension Function Example (`components/language-learn/src/com/zihao/language_learn/fsrs/idxdb_actions.cljs:69-97`)

Components provide extension functions that handle specific action types:

```clojure
(defn execute-effect [system _e action args]
  (case action
    :data/idxb-query (apply load-due-cards* system args)
    :data/idxb-command (apply repeat-card* system (first args) (rest args))
    :data/import-edn-file (let [file (first args)
                                {:keys [store execute-actions]} system]
                            ...)
    nil))

(defn execute-action [{:keys [store] :as system} e action args]
  (case action
    :card/load-due-cards (load-due-cards)
    :card/repeat-card (repeat-card store args)
    :flashcard/show-answer (show-answer)
    :card/import-edn-file (import-edn-file)
    nil))
```

### Current WebSocket Handler Implementations

#### jetty-main WebSocket Handler (`components/jetty-main/src/com/zihao/jetty_main/core.clj:85-100`)

The current `make-ws-handler` accepts query-handler and command-handler but uses hardcoded event IDs:

```clojure
(defn make-ws-handler [query-handler command-handler]
  (fn [stop-ch {:keys [ch-chsk] :as ws-server}]
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [event uid client-id ?data id ?reply-fn]} event-msg]
            (try
              (case id
                :chsk/ws-ping nil
                :test/echo (?reply-fn [id ?data])
                :data/query ((make-ws-query-handler query-handler) event-msg)
                :data/command ((make-ws-command-handler command-handler) event-msg)
                nil)
              (catch Exception e
                (u/log ::error :exception e))))
          (recur))))))
```

#### ws-server.clj Alternative Pattern (`components/jetty-main/src/com/zihao/jetty_main/ws_server.clj:41-58`)

An alternative pattern in `ws_server.clj` passes the entire system to handlers:

```clojure
(defn make-ws-command-handler [{:keys [command-handler] :as system}]
  (fn [{:keys [event uid client-id ?data id ?reply-fn]}]
    (let [command ?data
          result (command-handler system command)]
      (when ?reply-fn
        (?reply-fn result)))))

(defn make-ws-handler [system]
  (fn [stop-ch {:keys [ch-chsk] :as ws-server-arg}]
    (let [ws-command-handler (make-ws-command-handler system)
          ws-query-handler (make-ws-query-handler system)]
      ...)))
```

#### agent-web Extended WebSocket Handler (`bases/agent-web/src/com/agent-web/api.clj:19-84`)

The agent-web base shows a more complex WebSocket handler with custom event IDs:

```clojure
(defn make-ws-handler [{:keys [ws/ws-server agent/store cljpy/python-env]
                        :as system}]
  (fn [stop-ch {:keys [ch-chsk chsk-send! connected-uids]}]
    ...
    (go-loop []
      (let [[event-msg port] (async/alts! [ch-chsk stop-ch] :priority true)]
        (when (= port ch-chsk)
          (let [{:keys [?data id ?reply-fn]} event-msg]
            (try
              (case id
                :chsk/ws-ping nil
                :test/echo ...
                :data/query ...
                :data/command ...
                :ai/send-user-message (let [{:keys [content]} ?data]
                                        ;; Custom handling for AI messages
                                        ...)
                nil)
              ...))))))
```

This shows the pattern: custom WebSocket event IDs are hardcoded in the `case` statement, similar to how built-in actions are handled in `make-execute-f`.

## Code References

### Backend HTTP Query/Command Pattern
- `components/jetty-main/src/com/zihao/jetty_main/core.clj:21-41` - HTTP query/command handler makers
- `components/jetty-main/src/com/zihao/jetty_main/core.clj:66-83` - Route creation with handlers
- `components/jetty-main/src/com/zihao/jetty_main/interface.clj:1-9` - Interface exposing make-routes and make-handler
- `bases/web-app/src/com/zihao/web_app/api.clj:10-34` - Base composes component handlers with `or`

### Component Handler Examples
- `components/language-learn/src/com/zihao/language_learn/interface.clj:14-24` - Component interface with handlers
- `components/xiangqi/src/com/zihao/xiangqi/api.clj:6-22` - Simple command/query handler example
- `components/xiangqi/src/com/zihao/xiangqi/interface.cljc:409-419` - Component interface re-export

### Frontend Extension Pattern
- `components/replicant-main/src/com/zihao/replicant_main/replicant/utils.cljc:70-102` - make-interpolate with extensions
- `components/replicant-main/src/com/zihao/replicant_main/replicant/actions.cljs:78-107` - make-execute-f with extensions
- `components/replicant-main/src/com/zihao/replicant_main/interface.cljc:7-15` - Interface exposing make-execute-f and make-interpolate
- `bases/web-app/src/com/zihao/web_app/main.cljs:38-74` - SPA system configuration with extension functions

### Component Extension Function Examples
- `components/language-learn/src/com/zihao/language_learn/fsrs/idxdb_actions.cljs:69-106` - execute-effect and execute-action

### WebSocket Handlers
- `components/jetty-main/src/com/zihao/jetty_main/core.clj:85-100` - make-ws-handler with query/command handlers
- `components/jetty-main/src/com/zihao/jetty_main/ws_server.clj:26-58` - Alternative ws-server pattern
- `bases/agent-web/src/com/dx/agent_web/api.clj:19-84` - Extended WebSocket handler with custom events
- `bases/agent-web/src/com/dx/agent_web/main.cljs:15-78` - Frontend WebSocket event handling

## Architecture Documentation

### Extension Function Pattern

Both backend and frontend use a consistent extension pattern:

1. **Extension functions accept specific signatures**:
   - Backend handlers: `[system query/command]` -> return result or nil
   - Frontend interpolate: `[event case-key]` -> return value or nil
   - Frontend execute-actions: `[system event action args]` -> return actions or nil

2. **Composition with `or`**:
   - Multiple extension functions are composed using `or`
   - First non-nil result is used
   - Allows components to override or extend behavior

3. **Built-in fallback**:
   - After trying all extensions, fall back to built-in behavior
   - Usually via `case` statement for known types

4. **Integrant integration**:
   - Extension functions are passed via system config
   - `ig/init-key` methods compose extensions into the final handler

### WebSocket vs HTTP Handler Pattern

| Aspect | HTTP Query/Command | WebSocket Events |
|--------|-------------------|------------------|
| Handler accepts | `[system query/command]` | `[stop-ch ws-server]` |
| Event routing | Fixed `/api/query` and `/api/command` routes | Event `id` in `case` statement |
| Extension pattern | Component handlers composed with `or` at base level | Currently hardcoded in `case` |
| Reply mechanism | Return value becomes HTTP response | Optional `?reply-fn` callback |

## Historical Context (from architecture skill)

From `.claude/skills/architecture/SKILL.md`:

- **system**: Managed by integrant, contains stateful components like python-env, database connections, websocket channels
- **action**: Pure data describing what to do, with placeholders like `:event/form-data`
- **action-function**: Returns multiple actions, used in hiccup `:on` attributes
- **action-handler**: Executes effects, receives system and interpolated action
- **make-interpolate**: Converts placeholders to actual values, accepts extension functions for custom placeholders
- **view-function**: Takes state, returns hiccup

## Related Research

None found in thoughts/shared/research/

## Open Questions

1. Should WebSocket event handlers follow the same pattern as HTTP query/command handlers?
   - Current: Event IDs hardcoded in `case` statement
   - Alternative: Could use a registry pattern where components register their event handlers

2. How should bi-directional WebSocket communication be organized?
   - HTTP is request/response (query/command)
   - WebSocket can push events to clients without request
   - Current pattern uses custom event IDs (`:agent/emit-text`, `:agent/partial-result`, etc.)

3. Should `make-ws-handler` accept extension functions similar to `make-execute-f`?
   - Would allow components to register custom WebSocket event handlers
   - Consistent with frontend pattern
