class CodeActAgentDecision {
  message Message?
  code string? @description("The clojure code to execute")
}

function CodeActAgent(messages: Message[], code: string?, code_result: string?) -> CodeActAgentDecision {
  client "GLMCodingA"
  // client "LMStudioQWen3Coder"
  prompt #"
    {{ _.role("system") }}

    You are a CodeAct agent that can explore a Clojure codebase and execute code. Analyze the conversation history, the latest code execution, and its result to decide what action to take next. You can:
    1. Respond with a Message only (set message, leave code empty)
    2. Respond with a Message and code (set both message and code)
    3. Use code only without a message (leave message null, set code)

    You can write and execute Clojure code directly to explore the codebase, read files, search for patterns, and perform any other operations you need. Use standard Clojure functions and libraries to accomplish your tasks.

    IMPORTANT: You have access to predefined helper functions in the `com.dx.baml-client.tools.tools` namespace. You can use these functions in your code by requiring the namespace. The available functions are:

    ```clojure
    {{PredefinedFunctions()}}
    ```

    {{ ctx.output_format }}

    Conversation history:
    {% for msg in messages %}
    {{ _.role(msg.role) }} {{ msg.content }}
    {% endfor %}

    {{ _.role("system") }}
    {% if code %}
    The following code was just executed:
    ```clojure
    {{ code }}
    ```

    {% if code_result %}
    Code execution result:
    {{ code_result }}
    {% else %}
    Code execution completed (no result returned).
    {% endif %}
    
    Please analyze this result and decide what to do next. You may need to:
    - Provide a final answer if the task is complete
    - Execute additional code if more work is needed
    - Fix errors if the code failed
    - Continue with the next step in a multi-step process
    {% elif code_result %}
    Previous code execution result:
    {{ code_result }}
    
    Please analyze this result and decide what to do next.
    {% else %}
    No code has been executed yet. Based on the conversation history, decide what action to take next.
    {% endif %}
  "#
}

test codeact_list_directory {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "what is in current dir"
    }]
  }
}

test codeact_after_ls_execution {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "how many files in current dir"
    }]
    code "(ls)"
    code_result "file1.clj\nfile2.clj\ndirectory1\ndirectory2"
  }
  @@assert({{ this.message is not none and "2" in this.message.content }})
}

test codeact_list_namespace_functions {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "what functions in the current namespace"
    }]
  }
  @@assert({{ this.code is not none and ("ns-publics" in this.code or "ns-map" in this.code or "*ns*" in this.code) }})
}

test codeact_explicit_create_celsius_to_fahrenheit {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "create a celsius to fahrenheit function"
    }]
  }
  @@assert({{ this.code is not none and ("defn" in this.code or "def" in this.code) and ("celsius" in this.code or "Celsius" in this.code or "fahrenheit" in this.code or "Fahrenheit" in this.code or "9/5" in this.code or "* 9" in this.code or "+ 32" in this.code) }})
}

test codeact_use_predefined_read_file {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "read the file /path/to/example.clj"
    }]
  }
  @@assert({{ this.code is not none and ("read-file" in this.code or "com.dx.baml-client.tools.tools" in this.code or "baml-client.tools.tools" in this.code) }})
}

test codeact_use_predefined_read_file_retry {
  functions [CodeActAgent]
  args {
    messages [{
      role "user"
      content "read the file /path/to/example.clj"
    }
    {
      role "assistant"
      content #"
      ```clojure
      (require '[com.dx.baml-client.tools.tools :as tools])
      (tools/read-file "/path/to/example.clj")
      ```
      "#
    }
    {
      role "user"
      content "parse result error, if you want to execute code, please answer in JSON with key `code`"
    }
    ]
  }
  @@assert({{ this.code is not none and ("read-file" in this.code or "com.dx.baml-client.tools.tools" in this.code or "baml-client.tools.tools" in this.code) }})
}
