:page/title incremental UI
:blog-post/tags [:computation :UI]
:blog-post/author {:person/id :jan}
:page/body

主要是 UI 的 render 方向. 
model 通过 view function 转换成 view 的过程. 这个 view 可以是 v-dom, 也可以是 membrane 里面说的自定义的 IR.  
现在很多的前端库, 例如 react, 已经做了从 v-dom 到 dom 之间更新的 incremental, 用了各种 diff algorithm 之类的. 但是从 model 到 v-dom 或者其他 IR, 没有什么人做. 这里面的难度会比 v-dom 到 dom 更难, 因为 model 和 IR 都是用户自定义的, 而不是像 dom 那样是比较标准的东西. 所以需要提供 interface 到 user land. 更难抽象.  

## 提供给 user land 的 combinator
### map
map :: Incr a -> (a -> b) -> Incr b  

这个 operation 可以用来做 fan out, 例如一个 model, map 到不同的 subset, 转换成不同的 IR.  
```
       --- sub1---sub11
      /  
model/
     \
      \--- sub2---sub21
```
当 model 发生变化的时候, notify 所有的 child, child 计算新的值, 看和之前的对比有没有变化, 如果没变化, 就可以 cut off 这个分支. 如果有变化, 就 propogate 到下游.

### ap / map2
map2 :: Incr a -> Incr b -> (a -> b -> c) -> Incr c

这个 operator 可以用来做合并
```
       --- sub1---sub11
      /                \
model/                  \map2 sub11 sub21 f
     \                  /
      \--- sub2---sub21/
```

### bind
bind :: Incr a -> (a -> Incr b) -> Incr b

这个可以用作根据 Incr 的输出 a, 动态的选择一个 Incr computation 是返回 b 的.  
map 和 map2 是静态构建图, 这个可以构建个动态的图.  

### IncrMap 
IncrMap 是一种 kv map, 和普通 kv 的区别在于, 

1. value 存的是 
2. map 时候的表现.  
普通的 kv map, 就是对所有的 v 都应用 f, 得到 k -> f(v).  
IncrMap 在 map 的时候, 
