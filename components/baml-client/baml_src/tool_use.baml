class Message {
  role "user" | "assistant" | "system"
  content string
}

class AddTool {
  name "add"
  a int
  b int
}

class SubtractTool {
  name "subtract"
  a int
  b int
}

class ToolCall {
  name string
  args string
  result string
}

class ActionDecision {
  message Message?
  tools (AddTool | SubtractTool)[]
}

function DecideAction(messages: Message[], tool: ToolCall?) -> ActionDecision {
  client "LMStudioQWen3Coder"
  prompt #"
    {{ _.role("system") }}

    Analyze the conversation history and decide what action to take. You can:
    1. Respond with a Message only (set message, leave tools empty)
    2. Respond with a Message and use tools (set both message and tools)
    3. Use tools only without a message (leave message null, set tools)

    Available tools:
    - AddTool: Adds two numbers (a and b)
    - SubtractTool: Subtracts two numbers (a and b)

    {{ ctx.output_format }}

    Use tools when the user's request requires calculations. Include a message if you need to provide context, explanations, or conversational responses.

    Conversation history:
    {% for msg in messages %}
    {{ _.role(msg.role) }} {{ msg.content }}
    {% endfor %}

    {{ _.role("system") }}
    {% if tool %}
    A tool was just executed:
    - Tool name: {{ tool.name }}
    - Tool arguments: {{ tool.args }}
    - Tool result: {{ tool.result }}
    
    Please process this result and respond appropriately. You may need to use additional tools or provide a final answer.
    {% endif %}
  
  "#
}

test decide_action_single_tool {
  functions [DecideAction]
  args {
    messages [{
      role "user"
      content "I want to add 1 and 2"
    }]
  }
  @@assert({{ this.tools|length == 1 }})
  @@assert({{ this.tools[0].name == "add" }})
}

test decide_action_multiple_tools {
  functions [DecideAction]
  args {
    messages [{
      role "user"
      content "I want to add 1 and 2, then subtract 3 from 4"
    }]
  }
  @@assert({{ this.tools|length >= 2 }})
}

test decide_action_with_tool_result {
  functions [DecideAction]
  args {
    messages [{
      role "user"
      content "I want to add 1 and 2"
    }]
    tool {
      name "add"
      args "{:a 1, :b 2}"
      result "3"
    }
  }
  @@assert({{ this.message is not none }})
  @@assert({{ this.message.content|length > 0 }})
}

test decide_action_second_turn {
  functions [DecideAction]
  args {
    messages [
    {
      role "user"
      content "hello"
    }
    {
      role "assistant"
      content "Hello! How can I help you today?"
    }
    {
      role "user"
      content "great"
    }]
    tool null
  }
}