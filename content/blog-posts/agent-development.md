language model (LM) 可以看成一个输入 token, 返回 token 的 non deterministic 函数.
agent 开发就是怎么 compose 多个 LM 函数.

在传统编程的时候, workflow 是提前固定的, function1 -> function2 -> function3 这样调用, 也有条件分支, loop 等. 聚焦到某一个具体的步骤, 需要的输入是什么, 输出是什么都是固定的, 可以直接看 lexical scope 看出来. 如果在这里面混杂了一些 LM procedure, 也不影响单个 procedure / function 的 reasoning, 输入是明确的, 读取 state 里面的某些数据, 返回固定类型的一些数据.

如果要给 LM 提供 codeAct 能力, 有点类似给其他团队 API 调用权限.
