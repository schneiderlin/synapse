(ns com.zihao.jetty-main.interface
  (:require 
   [com.zihao.jetty-main.core :as core]
   [com.zihao.jetty-main.ring-ws :as ring-ws]))

;; =============================================================================
;; Routes & Handler
;; =============================================================================

(defn make-routes 
  "Creates routes for the web application.
   Supports both Sente and Ring WebSocket servers.
   
   Options:
   - :ws-path - custom path for Ring WS (default: \"/ws\")"
  [system ws-server query-handler command-handler & {:keys [ws-path] :as opts}]
  (apply core/make-routes system ws-server query-handler command-handler (mapcat identity opts)))

(defn make-handler [routes & {:keys [public-dir]}]
  (core/make-handler routes :public-dir public-dir))

;; =============================================================================
;; Legacy Sente Handlers (backward compatible)
;; =============================================================================

(defn make-ws-handler-with-extensions
  "Creates a WebSocket handler with extension functions.
   Extension functions are tried first before built-in event handling.
   Each extension should accept [system event-msg] and return non-nil if handled.
   
   NOTE: This is Sente-specific. For new code, use make-unified-ws-handler."
  [& extension-fns]
  (apply core/make-ws-handler-with-extensions extension-fns))

;; =============================================================================
;; Unified WebSocket API
;; =============================================================================

(defn make-ring-ws-server
  "Creates a Ring WebSocket server state.
   Returns a map with:
   - :sockets - atom of {client-id socket}
   - :ch-recv - core.async channel for incoming messages
   - :type - :ring-ws
   
   Options:
   - :format - :edn (default) or :json for message serialization"
  [& {:keys [format] :as config}]
  (apply ring-ws/make-ring-ws-server (mapcat identity config)))

(defn ws-adapter
  "Auto-detect and create the appropriate WebSocket adapter.
   Works with both Sente servers and Ring WebSocket servers.
   
   Returns unified context with:
   - :send! (fn [client-id event data])
   - :broadcast! (fn [event data])
   - :clients - deref-able collection
   - :ch-recv - channel for messages
   - :type - :sente or :ring-ws"
  [ws-server]
  (core/ws-adapter ws-server))

(defn make-sente-adapter
  "Creates a unified WebSocket context from a Sente server."
  [sente-server]
  (core/make-sente-adapter sente-server))

(defn make-ring-ws-adapter
  "Creates a unified WebSocket context from a Ring WebSocket server."
  [ring-ws-server]
  (core/make-ring-ws-adapter ring-ws-server))

(defn make-unified-ws-handler
  "Creates a unified WebSocket event handler.
   
   Arguments:
   - handler-fn: (fn [ws-ctx message]) - your business logic handler
     - ws-ctx has: :send!, :broadcast!, :clients
     - message has: :event, :data, :client-id, :reply!
   
   Returns a function (fn [stop-ch ws-adapter]) that starts the event loop.
   
   Example:
   ```clojure
   (defn my-handler [ws-ctx msg]
     (case (:event msg)
       :user/ping ((:reply! msg) {:pong true})
       :user/chat ((:broadcast! ws-ctx) :chat/message (:data msg))
       nil))
   
   ;; Using Ring WebSocket
   (def ws-server (jm/make-ring-ws-server))
   (def adapter (jm/ws-adapter ws-server))
   (def handler (jm/make-unified-ws-handler my-handler))
   (handler stop-ch adapter)
   
   ;; Using Sente (same handler works!)
   (def ws-server (ws-server/make-ws-server))
   (def adapter (jm/ws-adapter ws-server))
   (handler stop-ch adapter)
   ```"
  [handler-fn]
  (core/make-unified-ws-handler handler-fn))
