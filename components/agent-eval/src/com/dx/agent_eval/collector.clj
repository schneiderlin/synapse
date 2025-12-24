(ns com.dx.agent-eval.collector
  (:require [libpython-clj2.python :as py]
            [cheshire.core :as json]
            [com.dx.agent-eval.db :as db]))

(defn create-collector
  "Create a BAML Collector instance.
   system: map containing :cljpy/python-env key
   name: optional collector name (default 'agent-eval-collector')"
  ([system] (create-collector system "agent-eval-collector"))
  ([{:keys [cljpy/python-env]} name]
   (let [baml-py (py/import-module "baml_py")]
     (py/py. baml-py "Collector" name))))

(defn extract-log-data
  "Extract relevant data from Collector FunctionLog
   Returns map with :id, :function_name, :timestamp, :usage, :calls"
  [collector]
  (let [last-log (py/py.- collector "last")]
    (when last-log
      {:id (str (py/py.- last-log "id"))
       :function_name (py/py.- last-log "function_name")
       :timestamp (py/py.- (py/py.- last-log "timing") "start_time_utc_ms")
       :usage (py/->jvm (py/py.- last-log "usage"))
       :calls (py/->jvm (py/py.- last-log "calls"))})))

(defn extract-http-request-response
  "Extract HTTP request and response from Collector"
  [collector]
  (let [last-log (py/py.- collector "last")
        calls (py/py.- last-log "calls")
        last-call (py/py. calls "__getitem__" -1)]
    {:request (when last-call
                (let [http-req (py/py.- last-call "http_request")]
                  (when http-req
                    {:url (py/py.- http-req "url")
                     :method (py/py.- http-req "method")
                     :headers (py/->jvm (py/py.- http-req "headers"))
                     :body (py/py. (py/py.- http-req "body") "text")})))
     :response (py/py.- last-log "raw_llm_response")}))

(defn save-collector-log!
  "Save collector data to database"
  [collector & [session-id]]
  (let [log-data {} #_(extract-log-data collector)
        http-data (extract-http-request-response collector)
        timestamp (or (:timestamp log-data) (System/currentTimeMillis))
        session-id (or session-id "dummy-session")
        input-json (json/generate-string (:request http-data))
        output-json (json/generate-string (:response http-data))
        extra-json (json/generate-string
                    {:function_name (:function_name log-data)
                     :usage (:usage log-data)
                     :id (:id log-data)})]
    (db/insert-log! {:input input-json
                      :output output-json
                      :session_id session-id
                      :extra extra-json
                      :timestamp timestamp})))

(comment
  ;; Test creating a collector (requires system map with python-env)
  (def system {:cljpy/python-env (cljpy-main/make-python-env ["./components/baml-client"] {:baml-client "baml_client"})})
  (def coll (create-collector system "test-collector"))
  :rcf)
