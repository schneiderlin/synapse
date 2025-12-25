---
name: architechture
description: 本项目里面的各种架构设计, 设计中用到的各种概念例如 system, path, action, view-function, action-function, action-handler, base, component 等的解释
---

## base vs component
这是 polylith 工具里面的概念, base 是一个可以 deploy 的 artifact, 例如 webapp, cli, tui 等.
component 是 base 的 building block, component 会提供一个 interface namespace, 其他 base / component 只能直接 require interface, 不能 require component 里面的其他 namespace.

interface 需要用 defn 的方式暴露函数, 不能用 def 的方式, 并且需要有 docstring.

运行
```shell
clj -M:poly info
```
可以检查所有错误.

## system
