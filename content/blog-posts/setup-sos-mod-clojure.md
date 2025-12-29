:page/title Songs of syx MOD 使用 clojure
:blog-post/tags [:songs-of-syx :clojure]
:blog-post/author {:person/id :jan}
:page/body

只需要在游戏的进程里面, 创建一个 nrepl server, 然后就可以 jack in 到游戏进程里面. 通过 repl 进行各种试验.  

在 [MOD 环境搭建](/blog-posts/setup-sos-mod/) 里面说到, 游戏启动的时候会用 Class.forName 对所有 mod 提供的 class 都加载一次. 所以 clojure 生成的那些 class 也会被加载. 有一些 clojure 源码里面是用到了 set! dynvar 例如 `*warn-on-reflection*` 之类的, 在 clojure runtime 还没初始化的时候就 set! dynvar 是不可以的.  
目前绕过这个限制的方式是在打包 mod jar 的时候, 把 clojure 相关包的 scope 改成 provided. 并且把这些包直接放到安装目录 /jre/lib 里面. 这样就不会被 Class.forName 处理. 

clojure jack in 之后就可以直接获取游戏里面的各种数据了, 例如读取人口数据
```clojure
(ns repl.core
  (:import
   [game GAME] 
   [settlement.main SETT]
   [settlement.stats STATS]
   [settlement.room.main.construction ConstructionInit]
   [settlement.room.main.placement UtilWallPlacability]
   [settlement.room.main.placement PLACEMENT]
   [your.mod InstanceScript]))

(def player (GAME/player))
(def races (.races player))
(def race0 (.get races 0)) ;; 猪人
(def race1 (.get races 1)) ;; 邓多里安人

(.citizens player race0) ;; 猪人数量
(.citizens player race1)
```

后面会用不同的 blog 写
- [ ] 获取某个小人的信息
    - [ ] 年龄
    - [ ] 饥饿
    - [ ] 内急
    - [ ] 是否有住房
- [ ] 创建工地
- [ ] 创建墙
- [ ] 创建门(天花板)