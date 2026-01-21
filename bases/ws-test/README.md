# WebSocket Test Base

Test base for the unified WebSocket abstraction in `jetty-main`.

## Quick Start

### 1. Install Node.js dependencies

```bash
cd bases/ws-test
npm install
```

### 2. Start the Clojure server

In a Clojure REPL:

```clojure
(require '[com.zihao.ws-test.core :as ws-test])
(def server (ws-test/start-server))
```

Or with specific port:

```clojure
(def server (ws-test/start-server :port 4444))
```

### 3. Connect test clients

Terminal 1 (Alice):
```bash
cd bases/ws-test
node client.mjs alice
```

Terminal 2 (Bob):
```bash
cd bases/ws-test
node client.mjs bob
```

### 4. Test commands

In the client, type:

| Command | Description |
|---------|-------------|
| `ping` | Send ping, get pong reply |
| `echo hello` | Echo message back |
| `broadcast hi all!` | Broadcast to all clients |
| `send bob hey there` | Send to specific client |
| `clients` | List connected clients |
| `quit` | Exit |

### 5. Stop server

In REPL:

```clojure
(ws-test/stop-server server)
```

## Architecture

```
┌─────────────────┐     ┌─────────────────┐
│  Node.js Client │     │  Node.js Client │
│    (alice)      │     │     (bob)       │
└────────┬────────┘     └────────┬────────┘
         │                       │
         │    WebSocket (EDN)    │
         │                       │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │   Ring WS Handler     │
         │   /ws endpoint        │
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │  Unified WS Adapter   │
         │  (make-ring-ws-adapter)│
         └───────────┬───────────┘
                     │
         ┌───────────▼───────────┐
         │   Business Handler    │
         │   (ws-handler fn)     │
         └───────────────────────┘
```

## Files

- `core.clj` - Server code using unified WS API
- `client.mjs` - Node.js WebSocket client
- `package.json` - Node.js dependencies

