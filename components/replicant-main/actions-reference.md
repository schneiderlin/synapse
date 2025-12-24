# Built-in Actions Reference

This document describes the built-in actions provided by the Replicant Main component. These actions are available through the `make-execute-f` function and can be extended with custom action handlers.

## Core Actions

### State Management Actions

#### `:store/assoc`
Updates a key-value pair in the application store.

**Parameters:**
- `key` - The key to associate
- `value` - The value to associate with the key

**Example:**
```clojure
[:store/assoc :loading? true]
```

#### `:store/assoc-in`
Updates a nested value in the application store using a path.

**Parameters:**
- `path` - A vector path to the nested location
- `value` - The value to set at the path

**Example:**
```clojure
[:store/assoc-in [:user :profile :name] "John"]
```

#### `:store/update-in`
Updates a nested value in the application store using an update function.

**Parameters:**
- `path` - A vector path to the nested location
- `f` - Update function (takes current value, returns new value)
- `args` - Additional arguments to pass to the update function

**Example:**
```clojure
[:store/update-in [:counter] inc]
[:store/update-in [:items] conj new-item]
```

### Event Handling Actions

#### `:event/prevent-default`
Prevents the default browser behavior for an event.

**Example:**
```clojure
[:event/prevent-default]
```

#### `:event/stop-propagation`
Stops event propagation to parent elements.

**Example:**
```clojure
[:event/stop-propagation]
```

#### `:event/clear-input`
Clears the value of the input element that triggered the event.

**Example:**
```clojure
[:event/clear-input]
```

#### `:key/press`
Executes actions conditionally based on a specific key press.

**Parameters:**
- `key` - The key to match (e.g., "Enter", "Escape")
- `actions` - Actions to execute when the key matches

**Example:**
```clojure
[:key/press "Enter" [:data/command {:type :submit}]]
[:key/press "Escape" [:store/assoc :modal-open? false]]
```

### Navigation Actions

#### `:router/navigate`
Navigates to a new location in the application.

**Parameters:**
- `location` - Location map with `:location/page-id` and optional params

**Example:**
```clojure
[:router/navigate {:location/page-id :pages/user-profile
                   :location/params {:user-id 123}}]
```

### Data Actions

#### `:data/query`
Executes a query against the backend API.

**Parameters:**
- `query` - Query map to send to backend
- `options` - Optional map with `:on-success` callback

**Example:**
```clojure
[:data/query {:query/kind :user-list
              :query/params {:page 1}}
 {:on-success [:store/assoc :users :query/result]}]

[:data/query {:query/kind :user-details}
 {:on-success (fn [result] [:store/assoc :current-user result])}]
```

#### `:data/command`
Issues a command to the backend API.

**Parameters:**
- `command` - Command map to send to backend
- `options` - Optional map with `:on-success` callback

**Example:**
```clojure
[:data/command {:command/type :create-user
                :command/data {:name "John" :email "john@example.com"}}
 {:on-success [:router/navigate {:location/page-id :pages/users}]}]
```

#### `:data/choose-file`
Opens a file chooser dialog.

**Parameters:**
- `choose-args` - Arguments for file selection (currently unused)
- `options` - Optional map with `:on-success` callback

**Example:**
```clojure
[:data/choose-file {}
 {:on-success [:data/upload {:command/type :upload-image
                            :command/file :event/file}]}]
```

#### `:data/upload`
Uploads a file to the backend.

**Parameters:**
- `command` - Command map containing `:command/file` and other data
- `options` - Optional map with `:on-success` callback

**Example:**
```clojure
[:data/upload {:command/type :upload-profile-image
               :command/file :event/file
               :command/user-id 123}
 {:on-success [:store/assoc :upload-success? true]}]
```

### Utility Actions

#### `:debug/print`
Prints arguments to the browser console for debugging.

**Parameters:**
- `args` - Values to print to console

**Example:**
```clojure
[:debug/print "Current state" :event/form-data]
```

#### `:clipboard/copy`
Copies text to the system clipboard.

**Parameters:**
- `text` - Text to copy to clipboard

**Example:**
```clojure
[:clipboard/copy "https://example.com/share/123"]
```

## Interpolation

Actions support interpolation of special keywords that get replaced with runtime values:

- `:event/target.value` - Value of the event target (input value)
- `:event/target.int-value` - Input value parsed as integer
- `:event/target.checked` - Checkbox checked state
- `:event/clipboard-data` - Text from clipboard paste event
- `:event/target` - The event target DOM element
- `:event/form-data` - Form data as a map (for form events)
- `:event/event` - The raw event object
- `:event/file` - File from file input event
- `:query/result` - Result from a successful query

**Example:**
```clojure
[:data/command {:command/type :update-name
                :command/name :event/target.value}]
```

## Extending Actions

Custom actions can be added by passing extension functions to `make-execute-f`:

```clojure
(defn my-custom-action [system event action args]
  (case action
    :my-custom/operation (do-something system args)
    nil))

(def execute-fn (make-execute-f my-custom-action))
```

Extension functions receive:
- `system` - The application system map
- `event` - The DOM event (may be nil)
- `action` - The action keyword
- `args` - Action arguments

Extension functions should return:
- `nil` if they don't handle the action
- A result value if they handle the action
- A vector of actions to execute (for action chaining)