## Completion Candidates for @ References

### How It Works

When a user types `@` in the Textual TUI chat interface, the client calls a Clojure function to get completion candidates. The flow is:

1. **Client detects `@` trigger**: When user types `@` in the `CustomInput` widget, the client detects it
2. **Extract query**: Client extracts the text after `@` as the query string
3. **Call Clojure function**: Client calls `query-at-candidates` function in `resolve_at.clj` via the handler
4. **Server queries files**: Server uses `git ls-files` piped through `fzf` to filter matching files
5. **Server returns candidates**: Server returns a list of file paths
6. **Client displays autocomplete**: Client shows the list in a Textual dropdown/popup for selection
7. **Handle selection**: When user selects, insert the path into the message

### Architecture

**Client**: `components/agent-tui-cljpy/app.py`
- `CustomInput` widget extends Textual's `Input` widget
- Detects `@` character in input
- Calls Clojure handler function to get candidates
- Displays autocomplete results

**Server**: `components/agent/src/com/dx/agent/resolve_at.clj/resolve_at.clj`
- `query-at-candidates` function: Main entry point for querying file candidates
- Uses `babashka.process` to create pipelines
- Pipes `git ls-files` → `fzf` for fuzzy filtering

### Implementation Details

#### Server Function: `query-at-candidates`

**Location**: `resolve_at.clj`

**Signature**:
```clojure
(defn query-at-candidates
  "Query file candidates for @ completion.
   
   Args:
   - query: String to filter results (blank returns all)
   - opts: Optional map with:
     - :max-results: Maximum number of results (default: 50)
     - :workspace-root: Root directory for git (default: current working directory)
   
   Returns:
   - Vector of file path strings"
  [query & {:keys [max-results workspace-root] 
            :or {max-results 50 
                 workspace-root "."}}])
```

**Implementation Strategy**:
1. Use `babashka.process/pipeline` to chain processes
2. First process: `git ls-files` to get all tracked files
3. Second process: `fzf` with query for fuzzy filtering
4. Parse output and return as vector of strings

**Example using babashka.process**:
```clojure
(require '[babashka.process :refer [pipeline pb process check]])

(defn query-at-candidates [query]
  (let [p (pipeline 
            (pb "git" "ls-files")
            (pb "fzf" "-f" query))]
    (-> p
        last
        :out
        slurp
        str/split-lines
        (take 50))))
```

**Features**:
- **Git-based**: Only shows files tracked by git (respects `.gitignore` automatically)
- **Fuzzy filtering**: Uses `fzf` for fast, fuzzy matching
- **Case-insensitive**: `fzf` handles case-insensitive matching by default
- **Performance**: `git ls-files` is fast and only shows relevant files

#### Client Implementation

**Location**: `components/agent-tui-cljpy/app.py`

**CustomInput Enhancement**:
```python
class CustomInput(Input):
    """Custom Input widget that handles '@' key for file completion."""
    
    def on_key(self, event: events.Key) -> None:
        """Handle key events in the input."""
        if event.key == "escape":
            event.prevent_default()
            self.blur()
            return
        
        # Detect @ character for file completion
        current_value = self.value
        if "@" in current_value:
            # Extract query after @
            at_match = re.search(r'@([^\s]*)$', current_value)
            if at_match:
                query = at_match.group(1)
                # Call Clojure handler to get candidates
                # This would need to be integrated with the handler system
                self._show_completions(query)
    
    def _show_completions(self, query: str):
        """Show completion dropdown with file candidates."""
        # Implementation: Call Clojure handler, get results, show dropdown
        pass
```

**Integration with Handler**:
The Clojure handler (passed to `TerminalApp`) should expose a way to call `query-at-candidates`:
```clojure
(defn make-handler [query-at-candidates-fn]
  (fn [app message]
    ;; ... existing handler logic ...
    ;; When @ is detected, call:
    (let [candidates (query-at-candidates-fn query)]
      ;; Return candidates to Python client
      )))
```

### Response Format

The function returns a simple vector of file path strings:

```clojure
["components/agent/src/com/dx/agent/resolve_at.clj/resolve_at.clj"
 "components/agent-tui-cljpy/app.py"
 "readme.md"
 ...]
```

Paths are relative to the workspace root.

### Dependencies

**Required dependency**: Add `babashka/process` to `components/agent/deps.edn`:
```clojure
{:paths ["src" "resources"]
 :deps {obneyai/grain-core {...}
        babashka/process {:mvn/version "0.6.23"}}}
```

**System requirements**:
- `git` must be available in PATH
- `fzf` must be installed and available in PATH

### Performance Considerations

- **Git-based filtering**: `git ls-files` is fast and only shows tracked files
- **Fuzzy search**: `fzf` provides efficient fuzzy matching
- **Limit results**: Default max 50 results to avoid overwhelming the UI
- **No memoization needed**: `git ls-files` is fast enough for real-time queries
- **Pipeline efficiency**: Using `babashka.process/pipeline` ensures proper streaming without buffering issues

### Error Handling

- If `git` is not available, return empty vector or error message
- If `fzf` is not available, fall back to simple string filtering
- Handle cases where workspace is not a git repository
- Handle malformed queries gracefully

### Post-Selection Enhancements

#### Path Range Tracking

After a user selects a file path from the autocomplete dropdown, we need to track the position of the `@path` reference in the input for smart deletion.

**Implementation Approach**:

Store path range information (start and end character positions) when a path is selected. This metadata will be used for smart backspace deletion.

**Data Structure**:
```python
class CustomInput(Input):
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # List of (start, end) tuples for @path references
        # Updated when paths are inserted via autocomplete
        self._path_ranges = []
```

**Updating Path Ranges**:
- When `apply_completion` inserts a path, record the range: `(start_position, end_position)`
- When input changes (user types), update ranges to account for inserted/deleted characters
- When a path is deleted, remove its range from the list

**Example Implementation**:
```python
class AtMentionAutoComplete(AutoComplete):
    def apply_completion(self, completion: str, state: TargetState) -> None:
        """Apply completion and track path range."""
        input_widget = self.target
        value = state.text
        
        # Find the @ and everything after it
        match = re.search(r'@([^\s]*)$', value)
        if match:
            at_start = match.start()
            new_value = value[:at_start] + completion + " "
            input_widget.value = new_value
            
            # Record the path range (including @ and trailing space)
            path_start = at_start
            path_end = len(new_value)  # End is after the space
            input_widget._path_ranges.append((path_start, path_end))
            
            # Update cursor
            input_widget.cursor_position = len(new_value)
```

#### Smart Backspace Deletion

When the user presses backspace while the cursor is within or at the end of an `@path` reference, delete the entire path (including the `@`) as a single unit, rather than character-by-character.

**Implementation Strategy**:

1. **Intercept Backspace Key**: Override `on_key` in `CustomInput` to handle backspace specially
2. **Use Saved Path Ranges**: Check saved `_path_ranges` to see if cursor is in a path (no need to parse)
3. **Delete Entire Path**: Remove the whole `@path` including the `@` symbol
4. **Update Ranges**: Adjust remaining path ranges after deletion
5. **Update Cursor**: Position cursor where the path was

**Example Implementation**:
```python
class CustomInput(Input):
    def on_key(self, event: events.Key) -> None:
        """Handle key events, including smart backspace for @paths."""
        if event.key == "backspace":
            cursor_pos = self.cursor_position
            
            # Check if cursor is in any saved path range
            for i, (start, end) in enumerate(self._path_ranges):
                # If cursor is within the path range (including boundaries)
                if start <= cursor_pos <= end:
                    # Delete the entire @path
                    value = self.value
                    new_value = value[:start] + value[end:]
                    self.value = new_value
                    
                    # Remove this range and adjust other ranges
                    del self._path_ranges[i]
                    # Adjust ranges that come after the deleted one
                    deleted_length = end - start
                    for j in range(i, len(self._path_ranges)):
                        old_start, old_end = self._path_ranges[j]
                        self._path_ranges[j] = (old_start - deleted_length, old_end - deleted_length)
                    
                    # Position cursor where the path was
                    self.cursor_position = start
                    event.prevent_default()
                    return
            
            # If not in a path, let default backspace behavior handle it
            # But we still need to update path ranges for character deletion
            if self._path_ranges:
                # Adjust ranges for single character deletion
                deleted_pos = self.cursor_position - 1
                for i, (start, end) in enumerate(self._path_ranges):
                    if deleted_pos < start:
                        # Character deleted before this range, shift left
                        self._path_ranges[i] = (start - 1, end - 1)
                    elif start <= deleted_pos < end:
                        # Character deleted within a range, invalidate the range
                        del self._path_ranges[i]
                        # Adjust subsequent ranges
                        for j in range(i, len(self._path_ranges)):
                            old_start, old_end = self._path_ranges[j]
                            self._path_ranges[j] = (old_start - 1, old_end - 1)
                        break
        
        # Handle other keys normally (ESC, etc.)
        if event.key == "escape":
            event.prevent_default()
            self.blur()
            return
```

**Edge Cases to Handle**:
- Cursor at the start of `@path` → Delete the whole path
- Cursor in the middle of `@path` → Delete the whole path
- Cursor at the end of `@path` → Delete the whole path
- Cursor after `@path` with space → Normal backspace (delete space, update ranges)
- Multiple `@path` references → Delete only the one containing cursor
- `@path` at start of input → Delete path, cursor at position 0
- `@path` at end of input → Delete path, cursor at new end
- User deletes characters manually → Update/invalidate affected ranges

**Range Maintenance**:
- When text is inserted/deleted outside paths, adjust range positions
- When a character is deleted within a path range, invalidate that range
- Keep ranges in sorted order for efficient lookup

### Future Enhancements

- Support directory completion (not just files)
- Support multiple selections
- Cache recent selections
- Support absolute paths
- Add preview of file contents in dropdown
- Visual distinction for `@path` references in input (see Post-Selection Enhancements above)
- Smart backspace deletion for `@path` references (see Post-Selection Enhancements above)

