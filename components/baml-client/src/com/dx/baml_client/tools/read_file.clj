(ns com.dx.baml-client.tools.read-file
  (:require
   [clojure.java.io :as io]
   [clojure.string :as str]))

(defn- read-file-content-impl
  "Internal implementation for reading file content.
   
   Args:
   - path: Absolute file path (string)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)
   - throw-on-error: If true, throws exceptions on errors; if false, returns error messages
   
   Returns:
   - File content as string, or error message if throw-on-error is false and file doesn't exist
   - Throws exception if throw-on-error is true and file doesn't exist"
  [path line-range throw-on-error]
  (let [file (io/file path)]
    (if-not (.exists file)
      (if throw-on-error
        (throw (java.io.FileNotFoundException.
                (str "File not found: '" path "'")))
        (str "[Error: File not found]\n"
             "The file at path '" path "' does not exist.\n"
             "Please check the file path and try again."))
      (try
        (if line-range
          ;; Read specific line range
          (let [{:keys [start end]} line-range
                lines (with-open [reader (io/reader file)]
                        (doall (line-seq reader)))
                total-lines (count lines)
                start-1idx (or start 1)
                end-1idx (or end total-lines)
                start-0idx (max 0 (dec start-1idx))  ; Convert to 0-indexed, ensure >= 0
                end-1idx' (min end-1idx total-lines)  ; Ensure end doesn't exceed total
                end-0idx (dec end-1idx')  ; Convert to 0-indexed
                num-lines (max 1 (inc (- end-0idx start-0idx)))  ; Ensure at least 1 line
                selected-lines (take num-lines
                                     (drop start-0idx lines))]
            (str/join "\n" selected-lines))
          ;; Read entire file
          (slurp file))
        (catch java.io.FileNotFoundException e
          (if throw-on-error
            (throw e)
            (str "[Error: File not found]\n"
                 "The file at path '" path "' does not exist.\n"
                 "Please check the file path and try again.")))
        (catch Exception e
          (if throw-on-error
            (throw e)
            (str "[Error: Failed to read file]\n"
                 "An error occurred while reading the file at path '" path "'.\n"
                 "Error: " (.getMessage e))))))))

(defn read-file-content
  "Reads file content from the given absolute path, optionally limiting to a line range.
   Returns error messages instead of throwing exceptions.
   
   Args:
   - path: Absolute file path (string)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)
   
   Returns:
   - File content as string, or error message if file doesn't exist"
  [path & [line-range]]
  (read-file-content-impl path line-range false))

(def ^:private default-max-length 50000)

(defn refine-content
  "Refines file content by checking if it's too long and truncating if necessary.
   Adds helpful messages when content is truncated.
   
   Args:
   - content: File content string
   - max-length: Maximum content length before truncation (default: 50000)
   
   Returns:
   - Refined content string with truncation message if needed"
  ([content]
   (refine-content content default-max-length))
  ([content max-length]
   (if (<= (count content) max-length)
     content
     (let [truncated (subs content 0 max-length)
           message (str "\n\n[... Content truncated due to length ...]\n"
                       "The file content is too long to display in full.\n"
                       "To read specific sections, use the line-range parameter.\n"
                       "Example: invoke-read-file with line-range {:start 1 :end 100}\n"
                       "Line numbers are 1-indexed (first line is 1).")]
       (str truncated message)))))

;; ============================================================================
;; Public API
;; ============================================================================

(defn invoke-read-file
  "Read and return refined file content.
   Returns error messages instead of throwing exceptions.
   
   Args:
   - path: Absolute file path (string, required)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)
   
   Returns:
   - Refined file content as string, or error message if file doesn't exist"
  [path & [line-range]]
  (-> (read-file-content path line-range)
      refine-content))

(defn invoke-read-file-or-throw
  "Read and return refined file content.
   Throws exceptions on errors (file not found, read errors, etc.).
   
   Args:
   - path: Absolute file path (string, required)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)
   
   Returns:
   - Refined file content as string
   
   Throws:
   - java.io.FileNotFoundException if file doesn't exist
   - Exception if file read fails"
  [path & [line-range]]
  (-> (read-file-content-impl path line-range true)
      refine-content))
