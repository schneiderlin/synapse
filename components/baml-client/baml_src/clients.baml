client<llm> GLMCodingA {
  provider "anthropic"
  options {
    base_url "https://api.z.ai/api/anthropic"
    api_key env.ZAI_API_KEY
    model "GLM-4.7"
  }
}

client<llm> FreeGLM45Air {
  provider "openai-generic"
  options {
    base_url "https://openrouter.ai/api/v1"
    api_key env.OPENROUTER_API_KEY
    model "z-ai/glm-4.5-air:free"
  }
}

client<llm> LMStudioQWen3Coder {
  provider "openai-generic"
  options {
    base_url "http://localhost:1234/v1"
    model "qwen/qwen3-coder-30b"
  }
}

// client<llm> LMStudioQWen34B {
//   provider "openai-generic"
//   options {
//     base_url "http://localhost:1234/v1"
//     model "qwen/qwen3-coder-30b"
//   }
// }

// https://docs.boundaryml.com/docs/snippets/clients/retry
retry_policy Constant {
  max_retries 3
  strategy {
    type constant_delay
    delay_ms 200
  }
}

retry_policy Exponential {
  max_retries 2
  strategy {
    type exponential_backoff
    delay_ms 300
    multiplier 1.5
    max_delay_ms 10000
  }
}
