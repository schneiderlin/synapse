(ns com.zihao.baml-client.interface
  (:require
   [com.zihao.baml-client.core :as core]
   [com.zihao.baml-client.tools.tools :as tools]))

(defn reload []
  (core/reload))

(defn read-file
  "Read and return refined file content.

   Args:
   - path: Absolute file path (string, required)
   - line-range: Optional map with :start and :end keys (both integers, 1-indexed)

   Returns:
   - Refined file content as string"
  [path & [line-range]]
  (tools/read-file path line-range))

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
  (let [read-file-impl (require '[com.zihao.baml-client.tools.read-file :as read-file])]
    ((resolve 'read-file/invoke-read-file-or-throw) path line-range)))
