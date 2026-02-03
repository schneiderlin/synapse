(ns com.zihao.jetty-main.ring-ws
  "Ring WebSocket adapter for the unified WebSocket abstraction.
   Provides the same capabilities as Sente but using Ring's native WebSocket API."
  (:require
   [ring.websocket :as ws]
   [clojure.edn :as edn]
   [clojure.string :as str]
   [clojure.core.async :as async]
   [cheshire.core :as json]))

(defn- serialize-data
  "Serialize data according to format. Supports :edn and :json."
  [data format]
  (case format
    :json (json/generate-string data)
    :edn (pr-str data)
    (pr-str data)))

(defn make-ring-ws-server
  "Creates a Ring WebSocket server state.
   Returns a map with:
   - :sockets - atom of {client-id socket}
   - :ch-recv - core.async channel for incoming messages
   - :type - :ring-ws (to distinguish from Sente)
   - :format - serialization format (:edn or :json)
   
   Options:
   - :format - :edn (default) or :json for message serialization"
  [& {:keys [format] :or {format :edn}}]
  {:sockets (atom {})
   :ch-recv (async/chan 100)
   :type :ring-ws
   :format format})

(defn- parse-client-id
  "Extract client-id from request query string.
   Expected format: ?client-id=xxx"
  [request]
  (when-let [qs (:query-string request)]
    (some->> (str/split qs #"&")
             (map #(str/split % #"="))
             (filter #(= (first %) "client-id"))
             first
             second)))

(defn- generate-client-id []
  (str (java.util.UUID/randomUUID)))

(defn- parse-message
  "Parse incoming message according to format. Supports :edn and :json.
   For JSON, event values like \"test/ping\" are converted to keywords :test/ping."
  [message format]
  (case format
    :json (let [parsed (json/parse-string message true)] ; true = keywordize keys
            ;; Convert event string to keyword (e.g., "test/ping" -> :test/ping)
            (if-let [event-str (:event parsed)]
              (assoc parsed :event (keyword event-str))
              parsed))
    :edn (edn/read-string message)
    (edn/read-string message)))

(defn make-ring-ws-handler
  "Creates a Ring handler for WebSocket upgrade requests.

    Arguments:
    - ws-server: the server state from make-ring-ws-server

    The handler:
    - Upgrades HTTP requests to WebSocket connections
    - Tracks connected clients in the :sockets atom
    - Puts received messages onto :ch-recv channel as normalized events"
  [{:keys [sockets ch-recv format] :or {format :edn}}]
  (println "[ring-ws] Creating handler with format:" format "sockets atom:" (System/identityHashCode sockets))
  (fn [request]
    (println "[ring-ws] Request to /ws, upgrade?" (ws/upgrade-request? request))
    (if (ws/upgrade-request? request)
      (let [client-id (or (parse-client-id request) (generate-client-id))]
        (println "[ring-ws] Client ID:" client-id)
        {::ws/listener
         {:on-open
          (fn [socket]
            (println "[ring-ws] on-open called for" client-id "sockets atom:" (System/identityHashCode sockets))
            (swap! sockets assoc client-id socket)
            (println "[ring-ws] After swap, sockets:" @sockets)
            (async/put! ch-recv {:id :ws/open
                                 :client-id client-id
                                 :socket socket}))

          :on-message
          (fn [socket message]
            (try
              (let [parsed (parse-message message format)
                    event-id (or (:event parsed) :ws/message)]
                (async/put! ch-recv {:id event-id
                                     :client-id client-id
                                     :?data (:data parsed)
                                     :raw-message message
                                     :socket socket}))
              (catch Exception e
                (async/put! ch-recv {:id :ws/parse-error
                                     :client-id client-id
                                     :raw-message message
                                     :error e}))))

          :on-close
          (fn [_socket status-code reason]
            (swap! sockets dissoc client-id)
            (async/put! ch-recv {:id :ws/close
                                 :client-id client-id
                                 :status-code status-code
                                 :reason reason}))

          :on-error
          (fn [_socket error]
            (swap! sockets dissoc client-id)
            (async/put! ch-recv {:id :ws/error
                                 :client-id client-id
                                 :error error}))}})
      ;; Not a WebSocket upgrade request
      {:status 400
       :body "WebSocket upgrade required"})))

(defn send!
  "Send a message to a specific client.
   Message will be serialized according to :format (:edn or :json)."
  [{:keys [sockets format] :or {format :edn}} client-id event data]
  (when-let [socket (get @sockets client-id)]
    (try
      (ws/send socket (serialize-data {:event event :data data} format))
      (catch Exception _
        ;; Socket may have closed, remove it
        (swap! sockets dissoc client-id)))))

(defn broadcast!
  "Send a message to all connected clients.
   Message will be serialized according to :format (:edn or :json)."
  [{:keys [sockets format] :or {format :edn}} event data]
  (let [message (serialize-data {:event event :data data} format)]
    (doseq [[client-id socket] @sockets]
      (try
        (ws/send socket message)
        (catch Exception _
          (swap! sockets dissoc client-id))))))

(defn close!
  "Close a specific client's connection."
  [{:keys [sockets]} client-id]
  (when-let [socket (get @sockets client-id)]
    (ws/close socket)
    (swap! sockets dissoc client-id)))

(defn connected-clients
  "Returns set of connected client IDs."
  [{:keys [sockets]}]
  (set (keys @sockets)))

