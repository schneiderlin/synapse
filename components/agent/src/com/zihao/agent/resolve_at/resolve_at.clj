(ns com.zihao.agent.resolve-at.resolve-at
  (:require
   [com.zihao.baml-client.tools.read-file :as read-file]
   [babashka.process :refer [pipeline pb process check]]
   [clojure.string :as str]))

(defn resolve-at
  "Converts user input vector to a plain string.
   
   The input vector can contain:
   - Plain text strings
   - File context maps with {:type :file :path \"path/to/file\"}
   
   File contexts are resolved by reading the file content and inserting it
   into the resulting string.
   
   Args:
   - input: Vector containing strings and/or file context maps
   
   Returns:
   - Plain string with file contents resolved and concatenated
   
   Example:
   (resolve-at [\"看一下\" {:type :file :path \"./todo.md\"} \"里面有什么未完成的\"])
   => \"看一下<file content>里面有什么未完成的\""
  [input]
  (->> input
       (map (fn [item]
              (cond
                (string? item)
                item
                
                (and (map? item)
                     (= (:type item) :file)
                     (:path item))
                (let [file-path (:path item)
                      file-content (read-file/invoke-read-file-or-throw file-path)]
                  (str "\n--- File: " file-path " ---\n"
                       file-content
                       "\n--- End of " file-path " ---\n"))
                
                :else
                (str item))))
       (apply str)))

(defn query-at-candidates
  "Query file candidates for @ completion.
   
   Uses git ls-files piped through fzf for fuzzy filtering.
   Only returns files tracked by git (respects .gitignore automatically).
   
   Args:
   - query: String to filter results (blank returns all)
   - opts: Optional map with:
     - :max-results: Maximum number of results (default: 50)
     - :workspace-root: Root directory for git (default: current working directory)
   
   Returns:
   - Vector of file path strings (relative to workspace root)"
  [query & {:keys [max-results workspace-root]
            :or {max-results 50
                 workspace-root "."}}]
  (try
    ;; Check if git is available and workspace is a git repo
    (check (process ["git" "rev-parse" "--git-dir"] {:dir workspace-root}))
    
    (if (str/blank? query)
      ;; If query is empty, use git ls-files directly (faster)
      (let [result (process ["git" "ls-files"] {:dir workspace-root :out :string})
            output (:out @result)]
        (if output
          (->> (str/split-lines output)
               (take max-results)
               vec)
          []))
      
      ;; Otherwise, use fzf for fuzzy filtering 
      (try
        ;; Check if fzf is available
        (check (process ["fzf" "--version"]))
        
        ;; Build the pipeline: git ls-files | fzf -f query
        (let [git-proc (pb "git" "ls-files" {:dir workspace-root})
              ;; Use fzf with -f for filtering, --print0 for null-separated output,
              ;; --select-1 to auto-select if only one match, --exit-0 to exit with 0 even if no matches
              fzf-proc (pb "fzf" "-f" query "--print0" "--select-1" "--exit-0")
              pipeline-proc (pipeline git-proc fzf-proc)
              ;; Get the last process in the pipeline (fzf)
              last-proc (last pipeline-proc)
              ;; Wait for completion and get output
              result @last-proc
              output (:out result)]
          
          (if output
            (let [lines (-> output
                            slurp
                            (str/split #"\n")
                            (->> (remove str/blank?)
                                 (take max-results)
                                 vec))]
              lines)
            []))
        
        (catch Exception _e
          ;; If fzf is not available, fall back to simple string filtering
          (let [result (process ["git" "ls-files"] {:dir workspace-root :out :string})
                output (:out @result)
                query-lower (str/lower-case query)]
            (if output
              (->> (str/split-lines output)
                   (filter #(str/includes? (str/lower-case %) query-lower))
                   (take max-results)
                   vec)
              [])))))
    
    (catch Exception _
      ;; If git is not available, or not a git repo, return empty vector
      ;; In the future, could fall back to simple file system search
      [])))

(comment
  (println (resolve-at ["看一下" {:type :file :path "./readme.md"} "里面有什么未完成的"]))
  
  ;; Test query-at-candidates
  (query-at-candidates "agent")
  (query-at-candidates "")
  (query-at-candidates "clj" {:max-results 10})
  :rcf)
