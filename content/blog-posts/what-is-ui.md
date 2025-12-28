:page/title 可复用的 UI 代码
:blog-post/tags [:design :ui]
:blog-post/author {:person/id :jan}
:page/body


什么是 UI. 处理人和计算机交互, 有两条路线, 人 -> 计算机, 计算机 -> 人.

计算机 -> 人的路线.  
从 data 到 感观(视觉, 听觉等). 

人 -> 计算机的路线.  
用户输入(鼠标, 键盘, 触控等)映射到 user intent.

## 计算机 -> 人
先细分 计算机 -> 人 的路线.  
state data -> sensory data -> computer output

其中 state data 是需要可视化的数据, 例如一个用户表格.
sensory data 往往和 computer output 耦合到一起了. 例如 HTML dom tree 作为 sensory data, 和浏览器耦合了, 必须有浏览器才能渲染出页面. 各种游戏引擎的 SceneTree 作为 sensory data, 和相应的游戏引擎耦合了. Flutter 的 Widget tree, 和 Flutter 耦合了.

能不能把 sensory data 和如何使用sensory data(渲染到output) 解耦?  
可以通过引入中间层(intermedia representation)的方式.  
```
state data -> IR -> dom tree
                 -> widget tree
                 -> ...
```

需要自己根据业务情况建模, IR 表示什么数据.

引入 IR 解耦的好处是什么?  
好处一, 大量的 UI 业务逻辑在 state data -> IR 之间的转换. 并且IR 只是普通的数据(原子数据例如 string, number, 复合数据例如 Map, List, Set, 或者他们的嵌套). 那么这些业务逻辑很容易测试.

例如 state data 包含了
- 表格数据
- 筛选, 排序条件
- 分页参数

IR 只包含某一页符合条件排序好的数据, 只是一个 List of rows. 测试时很容易 assert IR 是否符合条件.

好处二, 如果 IR 包含了关于渲染的足够信息, 可以替换不同的 render backend. 
```
IR -> dom tree
   -> widget tree
   -> ...
```
这些胶水代码可以随意增加, 替换, 都不影响业务逻辑.

好处三, layout 更灵活.  
IR 里面一般也包含层级关系, 和 parent child sibling 之间应该怎么摆放的描述.
layout 指的是把这些 high level 的描述, 转换成简单的 x, y, width, height 之类的, 使得 render backend 可以直接使用.
例如
```
[:parent {:flexDirection "row"
          :width 100}
 [:child1 {:flexGrow 1}]
 [:child2 {:flexGrow 2}]]
```
转换成
```
[:parent {:flexDirection "row"
          :width 100}
 [:child1 {:width 33}]
 [:child2 {:width 66}]]
```
layout 算法不过是 data -> data 的纯函数转换, 完全可以在不同的平台之间复用. 也可以自己设计不同的 high level 语言描述视图元素之间关系. 可以转换到 low level 的 x y width height 几乎全部 render backend 都支持. 
也可以转译到例如 css 之类的其他描述语言利用已有生态.


## 人 -> 计算机
TODO
