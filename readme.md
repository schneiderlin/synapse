## 这是什么
我的全部公共代码, 用 mono repo 的方式管理.

## 如何使用
大部分是 clojure lib, 没有上传到 maven, 可以用 git url 的方式添加依赖
例如
```edn
linzihao/replicant-main {:git/url "https://github.com/schneiderlin/synapse.git"
                         :git/sha "7e7f42419b02019357c1b489198bc8da74a4ad2e"
                         :deps/root "components/replicant-main"}
```

## 其他

### 这是 AI slop 吗?
不是, 虽然有 AI 辅助, 但都是我人工验证过的代码.

### 为什么我要开源
在 AI 时代, 代码不值钱, 背后的 tacit knowledge 才值钱.   
开放代码出来让其他人引用, 提供 feedback, 交流学习才能发挥更大的价值

### 名字来源
Synapse
> In the nervous system, a synapse is a structure that allows a neuron (or nerve cell) to pass an electrical or chemical signal to another neuron or a target effector cell.

连接各种系统, 思想.