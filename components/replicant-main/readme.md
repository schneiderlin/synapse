# Replicant Main Component

A core component for handling Replicant-based frontend operations including routing, command processing, and state management.

## Overview

This component provides the main infrastructure for building reactive web applications using the Replicant framework. It handles client-side routing, command execution, and state querying in a unified manner.


## Architecture

### Glossary

- **Action** - Declarative operations that describe what should happen (e.g., `:store/assoc`, `:data/query`, `:router/navigate`). Actions are executed by the action executor and can modify state, trigger API calls, or handle user interactions.

- **View Function** - Pure functions that transform application state into UI descriptions. View functions read from the state but never modify it directly.

- **Action Function** - Functions that convert user events into actions. Given the current view and an event, they determine what actions should be executed (view, event) → actions.

- **State** - Immutable application state containing all data needed for the UI, including query results, command status, routing information, and UI state.

- **Store** - A container (typically an atom) that holds the current application state and provides controlled access for state updates through actions.

- **Router** - Client-side routing system that maps URLs to application locations, handles navigation, and manages browser history without full page reloads.

- **Location Load Actions** - Actions that are automatically executed when navigating to a new location. These actions typically fetch data needed for the new page or perform setup operations required for the target route.

- **Query** - Data fetching operations that retrieve information from the backend. Queries have lifecycle states (loading, success, error) and are cached in the state.

- **Command** - Operations that cause side effects on the backend (e.g., creating, updating, or deleting data). Commands track their execution status and results.

### Architecture Pattern

The component follows a unidirectional data flow architecture:

```
User Event → Action → State Update → View Re-render
     ↑                                      ↓
     └─────── Router ←──── URL Change ←─────┘
```

### Data Flow

1. **User interactions** trigger events
2. **Events** are converted to **actions** through action functions
3. **Actions** are executed, potentially:
   - Updating the **store** (state)
   - Making **queries** to fetch data
   - Issuing **commands** to modify backend data
   - Triggering **router** navigation
4. **State changes** cause view functions to recompute
5. **View functions** produce new UI descriptions
6. **Router** updates browser URL when location changes

## Integration

This component integrates with:
- Other Replicant components for UI rendering
- WebSocket components for real-time communication
- State management components for data persistence

## Replicant Application Architecture

### Component-Based Architecture

A typical Replicant application follows a modular architecture pattern:

```
Base Application (e.g., tiktok-web-app)
├── main.cljs          - Application configuration and system initialization
├── router.cljs        - Composes routes from all components
└── render.cljs        - Composes rendering from all components

Components (e.g., browser-device, adb-device)
├── interface.cljc    - Public API (render, execute-action, handlers)
├── router.cljc       - Component-specific routes and location load actions
├── render.cljs       - Component UI rendering functions
├── actions.cljc      - Component-specific action handlers
└── api.clj           - Backend API handlers (server-side)
```

### Composition Pattern

**Base Application** (`bases/tiktok-web-app/` or `bases/whatsapp-web-app/`):
- **main.cljs**: Configures the Replicant system using Integrant, composes all component execute-actions
- **router.cljs**: Combines routes from all components using `silk/routes` and `concat`
- **render.cljs**: Delegates to component render functions based on current page/route

**Business Components** (`components/browser-device/`, `components/adb-device/`):
- Separated by business logic/domain (e.g., browser management, ADB device management)
- Each component provides its own subset of router, API, render, and action functionality
- Components expose their functionality through a standardized interface

### Data Flow in Component Architecture

1. **Base router** composes all component routes
2. **Base render** delegates to appropriate component render function based on route
3. **Component actions** handle domain-specific operations
4. **Component APIs** handle backend operations for their domain
5. **Base main** composes all component execute-actions into unified action system

This architecture enables:
- **Modularity**: Each business domain is isolated in its own component
- **Composability**: Base applications can mix and match components
- **Separation of Concerns**: Clear boundaries between UI, routing, actions, and API logic
- **Reusability**: Components can be reused across different base applications

## Built-in Actions

For detailed documentation of built-in actions (state management, navigation, data operations, etc.), see [actions-reference.md](actions-reference.md).

## Lint (clj-kondo types)

The public API in `com.zihao.replicant-main.interface` carries Malli `{:malli/schema}` metadata so that:

- **Static lint**: Exported clj-kondo config gives consumers type hints (args/return) for interface functions.
- **Optional runtime**: Consumers can enable `malli.dev/start!` to validate calls at runtime.

### Regenerating the export

From this component directory:

```bash
clj -M:export
```

This runs Malli instrumentation, writes `.clj-kondo/metosin/malli-types-clj/config.edn`, and copies it to `resources/clj-kondo/clj-kondo.exports/com/zihao/replicant-main/config.edn`. Commit that file so dependents get the types.

### Using the types as a consumer

1. Add this component as a dependency.
2. Ensure a `.clj-kondo` directory exists, then copy configs from dependencies:  
   `clj-kondo --lint "$(clojure -Spath)" --copy-configs --skip-lint`  
   Configs are copied into `.clj-kondo/com/zihao/replicant-main/` and are **auto-loaded** (no `:config-paths` needed).
3. Enrich the cache: `clj-kondo --lint "$(clojure -Spath)" --dependencies --parallel`
4. Lint as usual, e.g. `clj-kondo --lint src`.

See [Take your linting game to the next level](https://tonitalksdev.com/take-your-linting-game-to-the-next-level) and [clj-kondo: Importing](https://cljdoc.org/d/clj-kondo/clj-kondo/doc/configuration#importing).
