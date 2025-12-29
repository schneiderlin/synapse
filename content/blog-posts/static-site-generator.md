:page/title static site generator
:blog-post/tags [:clojure]
:blog-post/author {:person/id :jan}
:page/body

传统的 static site generator, 少了数据库的环节. 大部分都是 parse content, 例如 markdown. 然后就渲染成页面.  
加一个数据库环节指的是, parse content 然后写入数据库, render 的时候从数据库里面读内容.

有多个好处
- SSG 和 SPA 之类的应用一样, 都是从数据库读内容, 然后 render, 有 potential 代码复用
- 可以在 content 里面嵌入 {{ select count(1) from blogs where tags = this.tag }} 之类的东西, 传统的方式不知道怎么做

content 和 DB, 可以看成是 代码 和 LSP server 的关系.
content 是 source of truth. LSP 只是提供 dev time 的便利.  

fdb 这个项目也是类似的套路, 但是他数据库直接就是 file system, 少了一些 leverage. 

## schema 和 ingest
可以是任何的 schema, 不同的 content 可以有不同的 schema. 例如 blog post 的 attribute 是 title, body, author, tags 之类的.  
各种不同的 raw content 格式, 如果都是 blog post, 可以写不同的 ingest function 转换成符合 schema 的.