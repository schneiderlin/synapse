import time
import asyncio
from app import TerminalApp

# Mock function to test @ completion (will be replaced with actual Clojure function later)
def mock_query_candidates(query: str):
    """Mock function that returns sample file candidates for testing.
    
    This will be replaced with the actual Clojure query-at-candidates function.
    """
    # Simulate some delay
    time.sleep(0.1)
    
    # Return mock file paths that match the query
    all_files = [
        "components/agent/src/com/dx/agent/resolve_at.clj/resolve_at.clj",
        "components/agent-tui-cljpy/app.py",
        "components/agent-tui-cljpy/app_main.py",
        "readme.md",
        "components/baml-client/src/com/dx/baml_client/tools/read_file.clj",
        "components/baml-client/baml_src/codeact.baml",
        "deps.edn",
        "components/agent/deps.edn",
        "components/agent/src/com/dx/agent/resolve_at.clj/plan.md",
    ]
    
    if not query:
        return all_files[:10]  # Return first 10 if no query
    
    # Simple substring matching (case-insensitive)
    query_lower = query.lower()
    matches = [f for f in all_files if query_lower in f.lower()]
    return matches[:20]  # Limit to 20 results

def fake_clojure_handler(self, message):
    """Fake Clojure handler for debugging the TUI.
    
    Args:
        self: TerminalApp instance
        message: User input message string
    """
    # Log: Handler started
    self.add_log(f"[INFO] Handler started with message: {message}")
    
    # Start spinner to show processing
    self.start_spinner()
    self.add_log("[INFO] Spinner started")
    
    # Add user message
    self.add_user_output(message)
    self.add_log(f"[INFO] User message added to chat: {message}")
    
    # Simulate some processing time
    self.add_log("[INFO] Simulating processing time...")
    time.sleep(0.5)
    self.add_log("[INFO] Processing complete")
    
    # Add assistant response using streaming
    self.add_log("[INFO] Starting streaming output...")
    self.start_streaming_output()
    response = f"echo: {message}"
    for char in response:
        self.append_streaming_output(char)
        time.sleep(0.01)  # Small delay to show streaming effect
    self.finish_streaming_output()
    self.add_log(f"[INFO] Streaming output finished: {response}")
    
    # Add a multiline code block example
    self.add_log("[INFO] Adding Clojure code block...")
    multiline_code = """(defn factorial [n]
  (if (<= n 1)
    1
    (* n (factorial (dec n)))))

(defn main []
  (println "Factorial of 5:" (factorial 5))
  (println "Factorial of 10:" (factorial 10)))"""
    
    self.add_clojure_code(multiline_code)
    self.add_log("[INFO] Code block added successfully")
    
    # Stop spinner
    self.stop_spinner()
    self.add_log("[INFO] Spinner stopped - Handler complete")

if __name__ == "__main__":
    app = TerminalApp(
        clojure_handler=fake_clojure_handler,
        query_candidates_fn=mock_query_candidates  # Mock function for testing @ completion
    )
    app.run()

