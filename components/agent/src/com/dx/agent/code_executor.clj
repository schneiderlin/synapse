(ns com.dx.agent.code-executor
  (:require
   [clojure.string :as str]
   [clojure.stacktrace :as stacktrace]
   [clojure-mcp-light.nrepl-eval :as nrepl-eval]
   [clojure.core :exclude [println print]]))

(defn code-executor
  "Execute Clojure code and return the result as a string.
   Uses nrepl-eval/eval-expr-with-timeout to execute code in a separate nREPL session.
   This avoids namespace issues and provides a clean execution environment."
  [code]
  (try
    ;; Ensure code is a string and trim it
    (let [code-str (if (string? code)
                     (str/trim code)
                     (str code))
          _ (when (empty? code-str)
              (throw (ex-info "Empty code string" {:code code})))
          nrepl-port 7888
          result (nrepl-eval/eval-expr-with-timeout
                  {:host "localhost"
                   :port nrepl-port
                   :expr code-str})]
      (if (instance? Exception result)
        (str "Error executing code:\n"
             "Error: " (.getMessage result)
             "\nException type: " (type result)
             "\nStack trace: " (with-out-str (stacktrace/print-stack-trace result)))
        (pr-str result)))
    (catch Exception e
      (let [code-str (if (string? code) (str/trim code) (str code))]
        (str "Code: " (pr-str code)
             "\nCode string: " (pr-str code-str)
             "\nError: " (.getMessage e)
             "\nException type: " (type e)
             "\nStack trace: " (with-out-str (stacktrace/print-stack-trace e)))))))

(comment
  "define a function convert celsius to fahrenheit"

  (def defn-code "(defn celsius->fahrenheit \n\"Converts a temperature from Celsius to Fahrenheit. \" \n  [c] \n  (+ (* c 9/5) 32))")

  (code-executor defn-code)
  (code-executor "(defn celsius->fahrenheit \n [c] (+ (* c 9/5) 32))")

  (code-executor nil)
  (code-executor "(+ 1 1)")

  (code-executor "(ls)")

  ;; 查看所有已加载的 namespace
  (code-executor "(all-ns)")

  ;; com.dx.agent.interface 里面有哪些 function
  (code-executor "(ns-publics 'com.dx.agent.interface)")

  (ns-publics 'com.dx.replicant-main.replicant.utils)

  :rcf)