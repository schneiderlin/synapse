max reader 是可以同时有多少个 reader 读 database file.
LMDB 理论上是不限制 reader 数量的, reader 之间不需要 coordinate.

reader 好像是占用一个 thread, 所以数量不能是无线. LMDB 层面做了限制.
datalevin 上面好像没有明确 manage reader 的代码. 

应该是 reader thread 没有正确关闭, database 的操作都是在 ring handler 里面处理的, 用的是 jetty 的线程模型.  

首先应该看一下 LMDB 当前的使用量. LMDB 是把 reader 的 thread ID 之类的信息写在 lock file 里面. 

open-kv 的时候打开了一个和 LMDB 的 connection, 每次去使用这个 LMDB 都是 reader / writer.



需要先搞清楚, 是不能 get-conn 还是不能 open-kv. 还是获取到的 db 不能用来查询
https://github.com/juji-io/datalevin/issues/326
这个 issue 挺清晰的, 复现出了问题, 是 dtlv 内部的问题, thread 释放之后没有对应把 reader slot 也释放出来. 所以 long running 的 process 总会遇到这个问题.