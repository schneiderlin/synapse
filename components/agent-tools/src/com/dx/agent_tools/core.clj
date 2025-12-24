(ns com.dx.agent-tools.core
  (:require
   [babashka.fs :as fs]
   [babashka.process :refer [process shell]]
   [clojure.string :as str]
   [cheshire.core :as json]))

(def workspace-root
  "The workspace root directory. Defaults to current working directory."
  (or (some-> (System/getenv "WORKSPACE_ROOT") fs/path)
      (fs/cwd)))

(defn- ensure-safe-path
  "Ensures the file path is within the workspace root for security."
  [file-path]
  (let [base-path (fs/path workspace-root)
        requested-path (fs/path file-path)
        normalized-path (if (fs/absolute? requested-path)
                          requested-path
                          (fs/path base-path requested-path))
        resolved-path (fs/normalize normalized-path)]
    (when (str/starts-with? (str resolved-path) (str base-path))
      resolved-path)))

(defn- parse-rg-result [output]
  (let [lines (str/split-lines output)
        ;; Parse JSON lines from ripgrep (each line is a JSON object)
        parsed-matches (->> lines
                            (keep (fn [line]
                                    (try
                                      (when-not (str/blank? line)
                                        (let [data (json/parse-string line true)]
                                          ;; Only process match lines (not summary lines)
                                          (when (= (:type data) "match")
                                            {:file (-> (:path (:data data))
                                                       (str/replace "\\" "/")
                                                       (str/replace "//" "/"))
                                             :line-number (:line_number (:data data))
                                             :line (get-in (:data data) [:lines :text])})))
                                      (catch Exception _
                                        nil))))
                            (remove nil?)
                            (group-by :file))]
    {:content (-> parsed-matches
                  (json/encode {:escape-non-ascii false})
                  (str/replace "\\" ""))
     :isError false}))

(defn grep-tool-fn [{:keys [pattern path type]
                     :as args}]
  (try
    (let [base-path (fs/path workspace-root)
          search-path (if path
                        (if-let [safe-path (ensure-safe-path path)]
                          safe-path
                          (throw (ex-info "Path outside workspace" {:path path})))
                        base-path)
          search-path-str (str search-path)
          ;; Build ripgrep command with JSON output for reliable parsing
          rg-args (cond-> ["rg" 
                           "-M" 200 ;; max line length
                           "--max-columns-preview"
                           "--json" pattern]
                    ;; Add type filter if specified
                    type (conj "-t" type)
                    ;; Add path (ripgrep will search in this directory)
                    true (conj search-path-str))
          ;; Execute ripgrep - use string path for :dir to avoid coercion issues
          result (try
                  (let [proc (process rg-args
                                      {:dir base-path
                                       :out :string
                                       :err :string})]
                    @proc)
                  (catch Exception e
                    {:exit -1
                     :out nil
                     :err (str "Failed to execute ripgrep: " (.getMessage e) " (class: " (class e) ")")})) 
          output (:out result)
          exit-code (:exit result)]
      (cond
        ;; No matches found (exit code 1 is normal for ripgrep when no matches)
        (or (= exit-code 1) (str/blank? output))
        {:content [{:type "text" :text (str "No matches found for pattern: " pattern)}]
         :isError false}
        
        ;; Error occurred
        (not= exit-code 0)
        (let [err-msg (try
                       (cond
                        (string? (:err result)) (:err result)
                        (nil? (:err result)) "No error message available"
                        :else (pr-str (:err result)))
                        (catch Exception e
                          (str "Error getting error message: " (.getMessage e))))]
          {:content [{:type "text" :text (str "Error during grep (exit code " exit-code "): " err-msg)}]
           :isError true})
        
        ;; Success - parse ripgrep JSON output
        :else (parse-rg-result output)))
    (catch Exception e
      {:content [{:type "text" :text (str "Error during grep: " (.getMessage e))}]
       :isError true})))

(comment
  (-> (grep-tool-fn {:pattern "TODO"
                 :path nil
                 :type nil})
      :content 
      type
      #_(json/encode {:escape-non-ascii false})
      #_(str/replace "\\" "") 
      )
  :rcf)

(defn list-dir-tool-fn [{:keys [target_directory]
                         :as args}]
  (try
    (if-let [safe-path (ensure-safe-path (or target_directory "."))]
      (if (fs/exists? safe-path)
        (if (fs/directory? safe-path)
          (let [entries (fs/list-dir safe-path)
                base-path (fs/path workspace-root)
                result (->> entries
                            (map (fn [entry]
                                   (let [rel-path (fs/relativize base-path entry)
                                         entry-type (cond
                                                      (fs/directory? entry) "dir"
                                                      (fs/regular-file? entry) "file"
                                                      (fs/sym-link? entry) "symlink"
                                                      :else "unknown")]
                                     (str entry-type "\t" rel-path))))
                            (sort)
                            (str/join "\n"))]
            {:content [{:type "text" :text result}]
             :isError false})
          {:content [{:type "text" :text "Path exists but is not a directory"}]
           :isError true})
        {:content [{:type "text" :text (str "Directory not found: " target_directory)}]
         :isError true})
      {:content [{:type "text" :text (str "Access denied: Path must be within workspace root")}]
       :isError true})
    (catch Exception e
      {:content [{:type "text" :text (str "Error listing directory: " (.getMessage e))}]
       :isError true})))

(comment
  (list-dir-tool-fn nil {:target_directory "."})
  :rcf)

(defn glob-tool-fn [{:keys [glob_pattern target_directory]
                     :as args}]
  (try
    (let [base-path (fs/path workspace-root)
          search-path (if target_directory
                        (if-let [safe-path (ensure-safe-path target_directory)]
                          safe-path
                          (throw (ex-info "Path outside workspace" {:path target_directory})))
                        base-path)
          files (fs/glob search-path glob_pattern)
          result (->> files
                      (filter fs/regular-file?)
                      (map (fn [file]
                             (let [rel-path (fs/relativize base-path file)
                                   file-type (cond
                                               (fs/directory? file) "dir"
                                               (fs/regular-file? file) "file"
                                               :else "unknown")]
                               (str file-type "\t" rel-path))))
                      (sort)
                      (str/join "\n"))]
      (if (seq result)
        {:content [{:type "text" :text result}]
         :isError false}
        {:content [{:type "text" :text (str "No files found matching pattern: " glob_pattern)}]
         :isError false}))
    (catch Exception e
      {:content [{:type "text" :text (str "Error during glob search: " (.getMessage e))}]
       :isError true})))

(comment
  (glob-tool-fn nil {:glob_pattern "**/*.clj"
                     :target_directory "."})
  :rcf)
