import os
import asyncio
import re
from concurrent.futures import ThreadPoolExecutor

# Disable BAML terminal logs to prevent interference with TUI
os.environ["BAML_LOG"] = "off"

from textual.app import App, ComposeResult
from textual.widgets import Input, LoadingIndicator, Static, RichLog
from textual.containers import Vertical, Container, Horizontal, ScrollableContainer
from textual.content import Content
from textual import events
from textual_autocomplete import AutoComplete
from textual_autocomplete._autocomplete import DropdownItem, TargetState
from rich.markdown import Markdown
from rich.syntax import Syntax
from rich.console import RenderableType, Console

css = """
  .main {
      padding: 1 2;
  }

  #chat-scroll {
      margin-bottom: 1;
      padding: 1;
      height: 1fr;
      overflow-y: auto;
      scrollbar-size-vertical: 1;
      scrollbar-visibility: visible;
  }

  #input {
      margin-top: 1;
  }

  .hidden {
      display: none;
  }

  #spinner {
      width: 3;
      height: 1;
  }

  .message {
      margin: 1 0;
      padding: 0 1;
      height: auto;
  }

  .user-message {
      border-left: solid $primary;
      padding-left: 2;
  }

  .assistant-message {
      border-left: solid $success;
      padding-left: 2;
  }

  .code-block {
      border-left: solid $accent;
      padding-left: 2;
      background: $surface;
  }

  #log-section {
      height: 10;
      border-top: solid $border;
      background: $panel;
  }

  #log-section.hidden {
      display: none;
  }

  #log-content {
      padding: 1;
  }

  /* AutoComplete styling with overlay and constrain */
  AtMentionAutoComplete {
      /* Customize the dropdown */
      & AutoCompleteList {
          max-height: 8;  /* The number of lines before scrollbars appear */
          color: $text-primary;  /* The color of the text */
          background: $panel;  /* The background color of the dropdown */
          border-left: wide $primary;  /* The color of the left border */
          overlay: screen;  /* Make it appear on top of everything */
          constrain: inside;  /* Keep it within screen bounds - will auto-adjust position */
      }

      /* Customize the matching substring highlighting */
      & .autocomplete--highlight-match {
          color: $text-accent;
          text-style: bold;
      }

      /* Customize the text the cursor is over */
      & .option-list--option-highlighted {
          color: $text-success;
          background: $primary 50%;  /* 50% opacity, blending into background */
          text-style: bold;  
      }
  }

  """


def make_at_mention_candidates_callback(query_fn):
    """Create a candidates callback function for @ mention autocomplete.
    
    Args:
        query_fn: Function that takes (query: str) and returns list of file paths
    
    Returns:
        A callback function that takes TargetState and returns list of DropdownItem
    """
    def candidates_callback(state: TargetState) -> list[DropdownItem]:
        """Get candidates for the current input value.
        
        Extracts the query after @ and calls query_fn to get file candidates.
        Returns candidates with @ prefix for display.
        
        Args:
            state: TargetState containing the input widget state
        
        Returns:
            List of DropdownItem objects (with @ prefix)
        """
        value = state.text
        
        # Extract query after @ (match @ followed by non-whitespace until end or space)
        match = re.search(r'@([^\s]*)$', value)
        if not match:
            return []
        
        query = match.group(1)
        
        # If query is empty, return empty list (don't show suggestions for just @)
        if not query:
            return []
        
        try:
            # Get candidates - query_fn is expected to be synchronous from Python's perspective
            # (Clojure functions called via libpython-clj2 are synchronous)
            candidates = query_fn(query)
            
            # Return candidates as DropdownItem objects with @ prefix for display
            if candidates:
                return [DropdownItem(f"@{c}") for c in candidates]
            
            return []
        except Exception:
            # On error, return empty list
            return []
    
    return candidates_callback


class AtMentionAutoComplete(AutoComplete):
    """AutoComplete widget customized for @ file mentions.
    
    Extends textual-autocomplete's AutoComplete to properly replace @query with selected file path.
    """
    
    def __init__(self, target, query_fn, executor=None):
        """Initialize the autocomplete.
        
        Args:
            target: The Input widget to attach to
            query_fn: Function that takes (query: str) and returns list of file paths
            executor: ThreadPoolExecutor (kept for compatibility, not used)
        """
        self.query_fn = query_fn
        self._executor = executor
        
        # Create candidates callback
        candidates_callback = make_at_mention_candidates_callback(query_fn)
        
        # Initialize AutoComplete with the callback
        super().__init__(target, candidates=candidates_callback)
    
    def apply_completion(self, completion: str, state: TargetState) -> None:
        """Apply the selected completion to the input widget.
        
        Override to properly replace @query with the selected file path and track the path range.
        
        Args:
            completion: The selected completion string (e.g., "@components/agent/...")
            state: TargetState containing the input widget state
        """
        input_widget = self.target
        value = state.text
        
        # Find the @ and everything after it
        match = re.search(r'@([^\s]*)$', value)
        if match:
            # Replace @query with the completion (which already includes @)
            at_start = match.start()
            at_end = match.end()  # End of the @query part
            new_value = value[:at_start] + completion + " "
            input_widget.value = new_value
            
            # Calculate inserted length (new path length - old query length)
            inserted_length = len(completion + " ") - (at_end - at_start)
            
            # Adjust existing ranges that come after the insertion point
            for i, (start, end) in enumerate(input_widget._path_ranges):
                if start >= at_start:
                    # This range comes after or at the insertion point, shift right
                    input_widget._path_ranges[i] = (start + inserted_length, end + inserted_length)
            
            # Record the new path range (including @ and trailing space)
            path_start = at_start
            path_end = at_start + len(completion + " ")
            input_widget._path_ranges.append((path_start, path_end))
            
            # Sort ranges by start position to keep them ordered
            input_widget._path_ranges.sort(key=lambda x: x[0])
            
            # Update prev_value for change detection
            input_widget._prev_value = new_value
            
            # Move cursor to end
            input_widget.cursor_position = len(new_value)
        else:
            # Fallback: just append the completion
            old_length = len(input_widget.value)
            input_widget.value = value + completion + " "
            
            # Record the path range
            path_start = old_length
            path_end = len(input_widget.value)
            input_widget._path_ranges.append((path_start, path_end))
            
            # Update prev_value for change detection
            input_widget._prev_value = input_widget.value
            
            input_widget.cursor_position = len(input_widget.value)


class CustomInput(Input):
    """Custom Input widget that handles ESC to blur and smart backspace for @paths."""
    
    def __init__(self, *args, **kwargs):
        super().__init__(*args, **kwargs)
        # List of (start, end) tuples for @path references
        # Updated when paths are inserted via autocomplete
        # Ranges are kept sorted by start position
        self._path_ranges = []
        # Track previous value for detecting insertions
        self._prev_value = self.value if hasattr(self, 'value') else ""
    
    def on_mount(self) -> None:
        """Initialize previous value tracking."""
        super().on_mount()
        self._prev_value = self.value
    
    def on_key(self, event: events.Key) -> None:
        """Handle key events in the input."""
        # Handle ESC to blur
        if event.key == "escape":
            event.prevent_default()
            self.blur()
            return
        
        # Handle smart backspace for @paths
        if event.key == "backspace":
            if self._handle_smart_backspace(event):
                return  # Smart backspace handled the deletion
        
        # For other keys, don't prevent default - let Input handle them normally
        # But we need to update path ranges for text changes
        if event.key not in ["escape", "tab", "enter"]:
            # Text will change, update ranges after default handling
            # We'll use on_input_changed to update ranges
            pass
    
    def _handle_smart_backspace(self, event: events.Key) -> bool:
        """Handle smart backspace for @path references.
        
        Returns True if backspace was handled (path deleted), False otherwise.
        """
        cursor_pos = self.cursor_position
        
        # Check if cursor is in any saved path range
        # Iterate in reverse to handle deletion correctly
        for i in range(len(self._path_ranges) - 1, -1, -1):
            start, end = self._path_ranges[i]
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
                # Update prev_value for next change detection
                self._prev_value = new_value
                event.prevent_default()
                return True
        
        # If not in a path, let default backspace behavior handle it
        # The range updates will be handled in on_input_changed after the deletion
        return False
    
    def on_input_changed(self, event: Input.Changed) -> None:
        """Update path ranges when input changes (for insertions/deletions outside paths)."""
        prev_value = getattr(self, '_prev_value', '')
        current_value = self.value
        
        if len(current_value) > len(prev_value):
            # Text was inserted
            inserted_length = len(current_value) - len(prev_value)
            # Find where the insertion happened by comparing strings
            insertion_point = 0
            for i in range(min(len(prev_value), len(current_value))):
                if prev_value[i] != current_value[i]:
                    insertion_point = i
                    break
            else:
                # Strings are identical up to min length, insertion is at the end
                insertion_point = len(prev_value)
            
            # Adjust ranges that come after the insertion point
            for i, (start, end) in enumerate(self._path_ranges):
                if start >= insertion_point:
                    # This range comes after the insertion, shift right
                    self._path_ranges[i] = (start + inserted_length, end + inserted_length)
        
        elif len(current_value) < len(prev_value):
            # Text was deleted (not handled by smart backspace)
            deleted_length = len(prev_value) - len(current_value)
            # Find where the deletion happened
            deletion_point = 0
            for i in range(min(len(prev_value), len(current_value))):
                if prev_value[i] != current_value[i]:
                    deletion_point = i
                    break
            else:
                # Strings are identical up to min length, deletion is at the end
                deletion_point = len(current_value)
            
            # Update ranges: shift left or invalidate if deletion overlaps
            # Iterate in reverse to handle deletion correctly
            for i in range(len(self._path_ranges) - 1, -1, -1):
                start, end = self._path_ranges[i]
                if deletion_point < start:
                    # Deletion before this range, shift left
                    self._path_ranges[i] = (start - deleted_length, end - deleted_length)
                elif start <= deletion_point < end:
                    # Deletion within this range, invalidate the range
                    del self._path_ranges[i]
                    # Adjust subsequent ranges
                    for j in range(i, len(self._path_ranges)):
                        old_start, old_end = self._path_ranges[j]
                        self._path_ranges[j] = (old_start - deleted_length, old_end - deleted_length)
                    break
                elif deletion_point >= end:
                    # Deletion after this range, no change needed
                    pass
        
        # Store current value for next comparison
        self._prev_value = current_value


class MessageWidget(Static):
    """Base class for message widgets."""
    
    def __init__(self, content: RenderableType, raw_text: str = "", *args, **kwargs):
        super().__init__(*args, **kwargs)
        # Convert Rich renderable to Textual Content for better selection support
        if isinstance(content, (Markdown, Syntax)):
            # For code blocks, prefer raw text for selection
            if isinstance(content, Syntax) and raw_text:
                textual_content = Content.from_text(raw_text)
            else:
                # Convert Rich renderable to Content while preserving formatting
                try:
                    console = Console(width=80, legacy_windows=False)
                    textual_content = Content.from_rich_text(content, console)
                except Exception:
                    # Fallback to plain text if conversion fails
                    if raw_text:
                        textual_content = Content.from_text(raw_text)
                    else:
                        # Render to plain text as last resort
                        console = Console(width=80, legacy_windows=False)
                        with console.capture() as capture:
                            console.print(content)
                        text = capture.get()
                        textual_content = Content.from_text(text)
            self.update(textual_content)
        else:
            self.update(content)


class UserMessage(MessageWidget):
    """Widget for user messages."""
    
    def __init__(self, content: RenderableType, raw_text: str = "", *args, **kwargs):
        super().__init__(content, raw_text, *args, **kwargs)
        self.add_class("message")
        self.add_class("user-message")


class AssistantMessage(MessageWidget):
    """Widget for assistant messages."""
    
    def __init__(self, content: RenderableType, raw_text: str = "", *args, **kwargs):
        super().__init__(content, raw_text, *args, **kwargs)
        self.add_class("message")
        self.add_class("assistant-message")


class CodeBlock(MessageWidget):
    """Widget for code blocks."""
    
    def __init__(self, content: RenderableType, raw_text: str = "", *args, **kwargs):
        super().__init__(content, raw_text, *args, **kwargs)
        self.add_class("message")
        self.add_class("code-block")


class TerminalApp(App):
    CSS = css
    BINDINGS = [
        ("l", "toggle_log", "Toggle Log"),
    ]

    def __init__(self, clojure_handler=None, conversation_history=None, query_candidates_fn=None):
        super().__init__()
        self.clojure_handler = clojure_handler
        self.conversation_history = conversation_history
        self.query_candidates_fn = query_candidates_fn  # Function to query file candidates
        self._executor = ThreadPoolExecutor(max_workers=1, thread_name_prefix="clojure-handler")
        self._streaming_widget = None  # Track the currently streaming message widget
        self._streaming_text = ""  # Accumulated text for streaming
        self._log_visible = False  # Track log visibility state

    def set_handler(self, handler_fn):
        self.clojure_handler = handler_fn
    
    def set_query_candidates_fn(self, query_fn):
        """Set the function to query file candidates for @ completion.
        
        Args:
            query_fn: Function that takes (query: str) and returns list of file paths
                     Can be async or sync. If sync, will be called in executor.
        """
        self.query_candidates_fn = query_fn

    def on_mount(self):
        # Set up autocomplete using textual-autocomplete
        if self.query_candidates_fn:
            input_widget = self.query_one("#input", CustomInput)
            # Create and mount the autocomplete widget
            autocomplete = AtMentionAutoComplete(
                target=input_widget,
                query_fn=self.query_candidates_fn,
                executor=self._executor
            )
            self.mount(autocomplete)
        
        # Load conversation history
        if self.conversation_history is not None:
            chat_container = self.query_one("#chat-scroll", Vertical)
            for message in self.conversation_history:
                if message["role"] == "user":
                    content = message['content']
                    markdown = Markdown(f"**User>** {content}")
                    widget = UserMessage(markdown, raw_text=content)
                    chat_container.mount(widget)
                elif message["role"] == "assistant":
                    content = message['content']
                    markdown = Markdown(content)
                    widget = AssistantMessage(markdown, raw_text=content)
                    chat_container.mount(widget)

    def action_toggle_log(self):
        """Toggle the log section visibility."""
        log_section = self.query_one("#log-section")
        if self._log_visible:
            log_section.add_class("hidden")
            self._log_visible = False
        else:
            log_section.remove_class("hidden")
            self._log_visible = True

    def action_blur_input(self):
        """Blur the input field."""
        input_widget = self.query_one("#input")
        input_widget.blur()

    def compose(self) -> ComposeResult:
        with Container(classes="main"):
            with Vertical():
                yield Vertical(id="chat-scroll")
                yield LoadingIndicator(id="spinner", classes="hidden")
                with ScrollableContainer(id="log-section", classes="hidden"):
                    yield RichLog(id="log-content", wrap=False, markup=True)
                yield CustomInput(id="input", placeholder="Type here...")

    async def on_input_submitted(self, event: Input.Submitted):
        """Async handler that runs the Clojure handler in background to keep UI responsive."""
        if event.input.id == "input" and event.value.strip():
            message = event.value.strip()
            event.input.clear()
            
            # Add user message immediately in the event loop thread before calling handler
            # This ensures it shows instantly (no need for call_from_thread)
            self.add_user_output(message)
            
            if self.clojure_handler:
                # Run the handler in a background thread to avoid blocking the event loop
                # This allows UI updates to show immediately
                loop = asyncio.get_event_loop()
                await loop.run_in_executor(self._executor, self.clojure_handler, self, message)

    def _add_widget_to_chat(self, widget):
        """Thread-safe helper to add a widget to the chat."""
        def _add_widget():
            chat_container = self.query_one("#chat-scroll", Vertical)
            chat_container.mount(widget)
        
        try:
            asyncio.get_running_loop()
            _add_widget()
        except RuntimeError:
            self.call_from_thread(_add_widget)
    
    def start_streaming_output(self):
        """Start streaming an assistant message. Creates a new widget for streaming."""
        def _start_streaming():
            # Create an empty assistant message widget for streaming
            markdown = Markdown("")
            widget = AssistantMessage(markdown, raw_text="")
            self._add_widget_to_chat(widget)
            self._streaming_widget = widget
            self._streaming_text = ""
        
        try:
            asyncio.get_running_loop()
            _start_streaming()
        except RuntimeError:
            self.call_from_thread(_start_streaming)
    
    def append_streaming_output(self, text_chunk: str):
        """Append a chunk of text to the currently streaming message."""
        def _append_streaming():
            if self._streaming_widget is None:
                # If no streaming widget exists, create one inline
                markdown = Markdown("")
                widget = AssistantMessage(markdown, raw_text="")
                self._add_widget_to_chat(widget)
                self._streaming_widget = widget
                self._streaming_text = ""
            
            # Accumulate the text
            self._streaming_text += text_chunk
            
            # Update the widget with the accumulated text
            markdown = Markdown(self._streaming_text)
            # Convert to Content for better selection support
            try:
                console = Console(width=80, legacy_windows=False)
                textual_content = Content.from_rich_text(markdown, console)
            except Exception:
                textual_content = Content.from_text(self._streaming_text)
            
            self._streaming_widget.update(textual_content)
        
        try:
            asyncio.get_running_loop()
            _append_streaming()
        except RuntimeError:
            self.call_from_thread(_append_streaming)
    
    def set_streaming_output(self, text: str):
        """Set the entire text of the currently streaming message (replaces existing content)."""
        def _set_streaming():
            if self._streaming_widget is None:
                # If no streaming widget exists, create one inline
                markdown = Markdown("")
                widget = AssistantMessage(markdown, raw_text="")
                self._add_widget_to_chat(widget)
                self._streaming_widget = widget
            
            # Set the text directly (replace existing)
            self._streaming_text = text
            
            # Update the widget with the new text
            markdown = Markdown(self._streaming_text)
            # Convert to Content for better selection support
            try:
                console = Console(width=80, legacy_windows=False)
                textual_content = Content.from_rich_text(markdown, console)
            except Exception:
                textual_content = Content.from_text(self._streaming_text)
            
            self._streaming_widget.update(textual_content)
        
        try:
            asyncio.get_running_loop()
            _set_streaming()
        except RuntimeError:
            self.call_from_thread(_set_streaming)
    
    def finish_streaming_output(self):
        """Finish streaming and finalize the message."""
        def _finish_streaming():
            if self._streaming_widget is not None:
                # Update the widget one final time with the complete text
                markdown = Markdown(self._streaming_text)
                try:
                    console = Console(width=80, legacy_windows=False)
                    textual_content = Content.from_rich_text(markdown, console)
                except Exception:
                    textual_content = Content.from_text(self._streaming_text)
                
                self._streaming_widget.update(textual_content)
                # Clear streaming state
                self._streaming_widget = None
                self._streaming_text = ""
        
        try:
            asyncio.get_running_loop()
            _finish_streaming()
        except RuntimeError:
            self.call_from_thread(_finish_streaming)
    
    def add_user_output(self, text):
        """Thread-safe method to add user message to the chat."""
        def _add_user_output():
            markdown = Markdown(f"**User>** {text}")
            widget = UserMessage(markdown, raw_text=text)
            self._add_widget_to_chat(widget)
        
        try:
            asyncio.get_running_loop()
            _add_user_output()
        except RuntimeError:
            self.call_from_thread(_add_user_output)

    def add_assistant_output(self, text):
        """Thread-safe method to add assistant message to the chat.
        For streaming, use start_streaming_output/append_streaming_output/finish_streaming_output instead."""
        def _add_assistant_output():
            markdown = Markdown(text)
            widget = AssistantMessage(markdown, raw_text=text)
            self._add_widget_to_chat(widget)
        
        try:
            asyncio.get_running_loop()
            _add_assistant_output()
        except RuntimeError:
            self.call_from_thread(_add_assistant_output)

    def add_clojure_code(self, code):
        """Thread-safe method to render Clojure code with syntax highlighting"""
        def _add_code():
            syntax = Syntax(code, "clojure", theme="monokai", line_numbers=False)
            widget = CodeBlock(syntax, raw_text=code)
            self._add_widget_to_chat(widget)
        
        try:
            asyncio.get_running_loop()
            _add_code()
        except RuntimeError:
            self.call_from_thread(_add_code)

    def add_plain_text(self, text):
        """Thread-safe method to render plain text"""
        def _add_text():
            textual_content = Content.from_text(text, markup=False)
            widget = Static(textual_content)
            widget.add_class("message")
            widget.add_class("assistant-message")
            self._add_widget_to_chat(widget)
        
        try:
            asyncio.get_running_loop()
            _add_text()
        except RuntimeError:
            self.call_from_thread(_add_text)

    def start_spinner(self):
        """Thread-safe method to start the spinner"""
        def _start_spinner():
            self.query_one("#spinner").remove_class("hidden")
        
        try:
            asyncio.get_running_loop()
            _start_spinner()
        except RuntimeError:
            self.call_from_thread(_start_spinner)

    def stop_spinner(self):
        """Thread-safe method to stop the spinner"""
        def _stop_spinner():
            self.query_one("#spinner").add_class("hidden")
        
        try:
            asyncio.get_running_loop()
            _stop_spinner()
        except RuntimeError:
            self.call_from_thread(_stop_spinner)

    def add_log(self, message: str):
        """Thread-safe method to add a log message to the log section."""
        def _add_log():
            log_content = self.query_one("#log-content", RichLog)
            log_content.write(message)
            # Auto-scroll to bottom
            log_section = self.query_one("#log-section")
            log_section.scroll_end(animate=False)
        
        try:
            asyncio.get_running_loop()
            _add_log()
        except RuntimeError:
            self.call_from_thread(_add_log)

