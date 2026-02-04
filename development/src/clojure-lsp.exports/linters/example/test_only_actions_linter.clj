(ns example.test-only-actions-linter
  "Example custom linter for detecting test-only action functions.
   Detects action functions when defined by a custom macro (e.g. defaction)
   and :action-defined-by is set in linter params.

   Usage:
   1. Define action functions with defaction (or similar macro):
      (defaction my-fn [state] ...)

   2. Set :action-defined-by #{'my.ns/defaction} in linter params.
      In .clj-kondo/config.edn use :lint-as {my.ns/defaction clojure.core/defn} so clj-kondo
      recognizes the macro; then :defined-by (or :defined-by->lint-as) will be my.ns/defaction."
  (:require [clojure-lsp.custom-linters-api :as api]
            [clojure.java.io :as io]))

;; -----------------------------------------------------------------------------
;; Helper Functions
;; -----------------------------------------------------------------------------

(defn defined-bys
  "Return set of namespaced symbols that defined this var (defined-by, defined-by->lint-as)."
  [definition]
  (set (filter identity [(:defined-by definition) (:defined-by->lint-as definition)])))

(defn action-function?
  "Check if a function definition is an action function.
   True when (params :action-defined-by) contains the var's :defined-by or
   :defined-by->lint-as (e.g. when defined by defaction macro)."
  [definition params]
  (let [action-defined-by-syms (get params :action-defined-by #{})]
    (some (fn [def-by]
            (some #(api/fast= def-by %) action-defined-by-syms))
          (defined-bys definition))))

;; -----------------------------------------------------------------------------
;; Main Functions
;; -----------------------------------------------------------------------------

(defn find-action-function-definitions
  "Find all action function definitions in the project.
   Action functions are identified by :action-defined-by (e.g. #{'my.ns/defaction}) in params."
  [db params]
  (let [all-vars (api/find-all-var-definitions db false)]  ; Only public vars
    (filter #(action-function? % params) all-vars)))

(defn build-usage-map
  "Build a map tracking usage of each action function in test vs production files."
  [action-defs db test-locations-regex]
  (let [internal-db (api/db-with-internal-analysis db)
        all-var-usages (->> (:analysis internal-db)
                            vals
                            (mapcat :var-usages)
                            (filter :name))]
    (loop [usage-map {}
           remaining-defs action-defs]
      (if (seq remaining-defs)
        (let [action-def (first remaining-defs)
              fq-name (symbol (str (:ns action-def)) (str (:name action-def)))
              simple-name (symbol (str (:name action-def)))
              usages (filter (fn [usage]
                               (let [usage-name (symbol (str (:name usage)))]
                                 (or (= fq-name usage-name)
                                     (= simple-name usage-name))))
                             all-var-usages)
              uri-in-test? (fn [uri] (some (fn [pattern] (re-find pattern uri)) test-locations-regex))
              test-uris (set (filter uri-in-test? (map :uri usages)))
              production-uris (set (remove uri-in-test? (map :uri usages)))]
          (recur (assoc usage-map action-def
                        {:test-uris test-uris
                         :production-uris production-uris})
                 (rest remaining-defs)))
        usage-map))))

(defn format-diagnostic-message
  "Format diagnostic message with test file locations.
   Uses interpose instead of clojure.string/join (not available in custom linter SCI context)."
  [action-def test-uris]
  (let [fq-name (symbol (str (:ns action-def)) (str (:name action-def)))
        test-file-strs (sort (map api/uri->filename test-uris))
        test-lines (apply str (interpose "\n" (map #(str "- " %) test-file-strs)))]
    (format "Test-only action function detected: '%s' is only used in test files and never in production code.\n\nTests should exercise production-used actions, not action functions created solely for testing.\n\nConsider:\n- Use this action function in production code, OR\n- Remove this action function and use an existing production action function in your test\n\nFound in tests:\n%s"
            fq-name
            test-lines)))

(defn lint
  "Main linter function. Uses full project (internal) analysis so we can see
   action definitions and their usages across all project files, not just the
   file(s) being linted (e.g. on didOpen we get a single uri but need test vs
   production usage from the whole project)."
  [{:keys [db params reg-diagnostic! analysis-type uris]}]
  (when-not (= :off (get params :level :info))
    (let [level (get params :level :info)
          exclude-fns (set (get params :exclude-fns []))
          exclude-ns-regex (get params :exclude-ns-regex nil)
          test-locations-regex (map re-pattern
                                    (get params :test-locations-regex
                                         ["_test\\.clj[a-z]?$"
                                          "/test/.*\\.clj$"]))
          ;; Use full project (internal) analysis for defs and usage map
          project-db (api/db-with-internal-analysis db)
          action-defs (find-action-function-definitions project-db params)
          usage-map (build-usage-map action-defs project-db test-locations-regex)]

      (doseq [[action-def {:keys [test-uris production-uris]}] usage-map]
        (when (and (seq test-uris)
                   (empty? production-uris)
                   (let [fq-name (symbol (str (:ns action-def)) (str (:name action-def)))]
                     (not (contains? exclude-fns fq-name)))
                   (or (nil? exclude-ns-regex)
                       (not (re-matches (re-pattern exclude-ns-regex)
                                        (str (:ns action-def))))))
          (let [fq-name (symbol (str (:ns action-def)) (str (:name action-def)))
                message (format-diagnostic-message action-def test-uris)]
            (reg-diagnostic!
             {:uri (:uri action-def)
              :range {:row (:name-row action-def)
                      :col (:name-col action-def)
                      :end-row (:name-end-row action-def)
                      :end-col (:name-end-col action-def)}
              :level level
              :message message
              :code "test-only-actions"
              :source "example/test-only-actions-linter"})))))))



