function ChatAgent(messages: Message[]) -> string {
  client "GLMCodingA"
  prompt #"
    {{ _.role("system") }}

    You are a chat agent that can chat with the user.
    {% for msg in messages %}
    {{ _.role(msg.role) }} {{ msg.content }}
    {% endfor %}
    "#
}

test chat_agent {
  functions [ChatAgent]
  args {
    messages [{
      role "user"
      content "hello"
    }]
  }
}
