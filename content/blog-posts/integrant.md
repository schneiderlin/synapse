:page/title integrant repl
:blog-post/tags [:clojure]
:blog-post/author {:person/id :jan}
:page/body

使用 powerpack 写静态博客的时候, 发现用到了 integrant repl. 但是我本地改代码或者改 content 文件都无法触发页面 reload. 猜测应该是 integrant repl 的问题.  
大致看了一下 integrant repl 的 readme. 大致就是把整个 system 放在一个 map 里面. 全部变量变成 map 里面的 local 变量. 核心思路是 system 可以像 docker container 一样, 当需要更新的时候, 不是把旧的 container state 改成新的 state. 而是把旧的 container 关掉, 起一个新 state 的 container. 