(ns com.zihao.replicant-main.export
  "Export Malli function schemas from interface to clj-kondo config for consumers.
   Run from component root: clj -M:export
   See: https://tonitalksdev.com/take-your-linting-game-to-the-next-level"
  (:require
   [clojure.java.io :as io]
   [malli.dev :as dev]
   [com.zihao.replicant-main.interface :as interface]))

(def export-dir
  "Target path under resources for clj-kondo export. Use namespace as single path segment (com.zihao.replicant-main) so --copy-configs lands at .clj-kondo/imports/com.zihao.replicant-main/ (two levels) and is auto-loaded."
  "resources/clj-kondo/clj-kondo.exports/com.zihao.replicant-main")

(defn export-types
  "Collect :malli/schema from interface, run Malli instrumentation to generate
   clj-kondo type config, then copy it to resources for publishing."
  []
  ;; Load interface so vars with :malli/schema are registered
  (assert (some? (resolve 'com.zihao.replicant-main.interface/make-execute-f))
          "interface namespace must be loaded")
  (dev/start!)
  (let [cache-file (io/file ".clj-kondo/metosin/malli-types-clj/config.edn")
        out-file   (io/file export-dir "config.edn")]
    (assert (.exists cache-file)
            (str "Malli did not write config; expected " (.getAbsolutePath cache-file)))
    (io/make-parents out-file)
    (io/copy cache-file out-file)
    (println "Exported types to" (str out-file)))
  (dev/stop!))
