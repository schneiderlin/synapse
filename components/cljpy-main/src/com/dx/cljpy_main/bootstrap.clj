(ns com.dx.cljpy-main.bootstrap
  (:require
   [babashka.process :refer [process check]]
   [babashka.fs :as fs]))

(defn check-uv-installed? 
  "Check if uv is installed on the current system"
  [] 
  (try
    (check (process ["uv" "--version"]))
    true
    (catch Exception _
      false)))

(comment
  (check-uv-installed?) 
  :rcf)

(defn install-uv 
  "Install uv based on the current operating system"
  [] 
  (let [os (System/getProperty "os.name")
        arch (System/getProperty "os.arch")]
    (cond
      ;; Windows
      (.contains os "Windows")
      (do
        (println "Installing uv on Windows...")
        (check (process ["powershell" "-Command" 
                         "irm https://astral.sh/uv/install.ps1 | iex"])))
      
      ;; macOS
      (.contains os "Mac")
      (do
        (println "Installing uv on macOS...")
        (check (process ["curl" "-LsSf" "https://astral.sh/uv/install.sh" "|" "sh"])))
      
      ;; Linux
      (.contains os "Linux")
      (do
        (println "Installing uv on Linux...")
        (check (process ["curl" "-LsSf" "https://astral.sh/uv/install.sh" "|" "sh"])))
      
      :else
      (throw (ex-info "Unsupported operating system for uv installation" 
                      {:os os :arch arch})))))

(comment
  (install-uv)
  :rcf)

(defn bootstrap 
  "Bootstrap the Python environment using uv" [] 
  (println "Bootstrapping Python environment...")
  
  ;; Check if .venv exists
  (if (fs/exists? ".venv")
    (println ".venv directory already exists")
    (do
      (println ".venv directory not found, setting up...")
      
      ;; Check if uv is installed
      (if (check-uv-installed?)
        (do
          (println "uv is installed, running uv sync...")
          (check (process ["uv" "sync"]))
          (println "Python environment setup complete"))
        (do
          (println "uv not found, installing...")
          (install-uv)
          (println "uv installed, running uv sync...")
          (check (process ["uv" "sync"]))
          (println "Python environment setup complete"))))))

(comment
  (bootstrap)
  :rcf)

