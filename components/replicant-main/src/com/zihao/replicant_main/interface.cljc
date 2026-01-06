(ns com.zihao.replicant-main.interface
  (:require
   #?(:cljs [com.zihao.replicant-main.replicant.actions :as actions])
   #?(:cljs [com.zihao.replicant-main.replicant.ws-client :as ws-client])
   [com.zihao.replicant-main.replicant.query :as query]
   [com.zihao.replicant-main.replicant.utils :as utils]))

(defn make-execute-f
  "Creates an execution function for handling actions with optional extensions.
   
   Parameters:
   - extension-fns: Optional functions to extend the execution behavior
   
   Returns: An execution function that can be used to process actions"
  [& extension-fns]
  #?(:cljs (apply actions/make-execute-f extension-fns)))

(defn get-result
  "Retrieves query results from the current application state.
   
   Parameters:
   - state: The current application state
   - query: The query to execute against the state
   
   Returns: The result of executing the query against the state"
  [state query]
  (query/get-result state query))

(defn make-interpolate
  "Creates an interpolation function for handling event data with optional extensions.
   
   Parameters:
   - extension-fns: Optional functions to extend the interpolation behavior.
                    Each function should take (event case-key) and return a value if handled, nil otherwise.
   
   Returns: An interpolation function that can be used to process event data"
  [& extension-fns]
  (apply utils/make-interpolate extension-fns))

(defn interpolate [event data]
  (utils/interpolate event data))

(defn parse-int [s]
  (utils/parse-int s))

(defn is-digit? [c]
  (utils/is-digit? c))

(defn gather-form-data [form-el]
  (utils/gather-form-data form-el))

(defn gen-uuid
  "Generates a random UUID string."
  []
  (utils/gen-uuid))

(defn make-ws-handler-with-extensions
  "Creates a WebSocket handler with extension functions (cljs only).
   Extension functions are tried first before built-in event handling.
   Each extension should accept [ws-client event-msg] and return non-nil if handled."
  [& extension-fns]
  #?(:cljs (apply ws-client/make-ws-handler-with-extensions extension-fns)
     :clj (throw (ex-info "make-ws-handler-with-extensions only available in ClojureScript" {}))))
