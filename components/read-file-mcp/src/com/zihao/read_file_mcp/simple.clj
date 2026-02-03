(ns com.zihao.read-file-mcp.simple
  (:require
   [co.gaiwan.mcp :as mcp]
   [co.gaiwan.mcp.state :as state]
   [malli.json-schema :as mjs]
   [babashka.fs :as fs]
   [clojure.string :as str]))

(def obsidian-dir "C:\\Users\\zihao\\Desktop\\workspace\\obsidian")

(defn- ensure-obsidian-path
  "Ensures the file path is within the obsidian directory for security."
  [file-path]
  (let [obsidian-path (fs/path obsidian-dir)
        requested-path (fs/path file-path)
        normalized-path (if (fs/absolute? requested-path)
                          requested-path
                          (fs/path obsidian-path requested-path))
        resolved-path (fs/normalize normalized-path)]
    (when (str/starts-with? (str resolved-path) (str obsidian-path))
      resolved-path)))

(defn- grep-in-file
  "Searches for pattern in a file and returns matches with line numbers."
  [file-path pattern]
  (try
    (when (fs/exists? file-path)
      (let [content (slurp (str file-path))
            lines (str/split-lines content)
            pattern-re (re-pattern pattern)]
        (->> lines
             (map-indexed (fn [idx line]
                            (when (re-find pattern-re line)
                              {:line-number (inc idx)
                               :line line})))
             (remove nil?)
             (seq))))
    (catch Exception _
      [])))

(defn- grep-tool-fn [_req {:keys [pattern file-pattern]}]
  (try
    (let [obsidian-path (fs/path obsidian-dir)
          ;; 默认搜索 md 如果不指定, 否则太慢了
          search-pattern (or file-pattern "**/*.md")
          files (fs/glob obsidian-path search-pattern)
          matches (->> files
                       (filter fs/regular-file?)
                       (keep (fn [file]
                               (let [file-matches (grep-in-file file pattern)]
                                 (when (seq file-matches)
                                   {:file (str (fs/relativize obsidian-path file))
                                    :matches file-matches}))))
                       (take 100))]
      (if (seq matches)
        (let [result (->> matches
                          (map (fn [match-entry] 
                                 (let [file (get match-entry :file)
                                       file-matches (get match-entry :matches)]
                                   (when (and file (seq file-matches))
                                     (str "File: " file "\n"
                                          (->> file-matches
                                               (map (fn [match]
                                                      (let [line-number (get match :line-number)
                                                            line (get match :line)]
                                                        (str "  " line-number ": " line))))
                                               (str/join "\n")))))))
                          (remove nil?)
                          (str/join "\n\n"))]
          {:content [{:type "text" :text result}]
           :isError false})
        {:content [{:type "text" :text (str "No matches found for pattern: " pattern)}]
         :isError false}))
    (catch Exception e
      {:content [{:type "text" :text (str "Error during grep: " (.getMessage e))}]
       :isError true})))

(comment 

  (grep-tool-fn nil {:file-pattern "**/*.md"
                     :pattern "TODO"})
  
  ;; this is super slow
  (grep-tool-fn nil {:file-pattern nil
                     :pattern "TODO"})
  :rcf)

(defn- read-file-tool-fn [_req {:keys [path]}]
  (try
    (if-let [safe-path (ensure-obsidian-path path)]
      (if (fs/exists? safe-path)
        (if (fs/regular-file? safe-path)
          (let [content (slurp (str safe-path))]
            {:content [{:type "text" :text content}]
             :isError false})
          {:content [{:type "text" :text "Path exists but is not a regular file"}]
           :isError true})
        {:content [{:type "text" :text (str "File not found: " path)}]
         :isError true})
      {:content [{:type "text" :text (str "Access denied: File must be within " obsidian-dir)}]
       :isError true})
    (catch Exception e
      {:content [{:type "text" :text (str "Error reading file: " (.getMessage e))}]
       :isError true})))

(comment
  (read-file-tool-fn nil {:path "zettelkasten/journals/2024_09_24.md"})
  (read-file-tool-fn nil {:path "zettelkasten\\journals\\2024_09_24.md"})
  :rcf)

;; Add a tool
(state/add-tool
 {:name "greet"
  :title "Greeting Tool"
  :description "Sends a personalized greeting"
  :schema (mjs/transform [:map [:name string?]])
  :tool-fn (fn [_req {:keys [name]}]
             {:content [{:type "text" :text (str "Hello, " name "!")}]
              :isError false})})

;; Grep tool
(state/add-tool
 {:name "grep"
  :title "Grep Tool"
  :description "Searches for a pattern in files within the obsidian directory"
  :schema (mjs/transform [:map
                          [:pattern string?]
                          [:file-pattern {:optional true} string?]])
  :tool-fn grep-tool-fn})

;; Read file tool
(state/add-tool
 {:name "read-file"
  :title "Read File Tool"
  :description "Reads a file from the obsidian directory"
  :schema (mjs/transform [:map [:path string?]])
  :tool-fn read-file-tool-fn})

(defn http [& args]
  (let [port (or (some-> (first args) Integer/parseInt)
                 (some-> (System/getenv "MCP_PORT") Integer/parseInt)
                 6000)]
    (println (str "Starting MCP server on port " port))
    (try
      (mcp/run-http! {:port port})
      (catch java.net.BindException _e
        (println (str "ERROR: Port " port " is already in use. Try a different port:"))
        (println "  Set MCP_PORT environment variable: $env:MCP_PORT=4001")
        (println "  Or pass as argument: clj -M -m com.zihao.read-file-mcp.simple 4001")
        (System/exit 1)))))

(defn stdio []
  (mcp/run-stdio! {}))

(defn -main [& _args]
  (http)
  #_(stdio))

(comment
  (def jetty (-main))
  :rcf)
