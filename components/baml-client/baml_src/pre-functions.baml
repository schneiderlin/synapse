template_string PredefinedFunctions() #"
(ns com.zihao.baml-client.tools.tools
  (:require
   [com.zihao.baml-client.tools.read-file :as read-file]))

(defn read-file
  "Read and return refined file content.
   
   Args:
   - path: Absolute file path (string, required)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)
   
   Returns:
   - Refined file content as string"
  [path & [line-range]]
  (read-file/invoke-read-file path line-range))
"#
