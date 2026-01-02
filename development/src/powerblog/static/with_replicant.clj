(ns powerblog.static.with-replicant)

(def page
  [:div
   "由于 replicant 只是一个 render library, 主要只是把 hiccup 变成 html. 所以也可以在后端使用, 只在 statis site generator 的 build phase 使用."
   [:br]
   "当前这个页面就是由 replicant 渲染的. 如何渲染 stateful 的组件? replicant 需要 watch 一个 atom, 根据 atom 变化重新渲染组件. 因此需要部分 JavaScript 代码, 创建 atom 和 watch"
   [:br]
   "后端渲染的 html, 只提供 attribute 标记, 表明这个 element 需要前端接管. 并且可能包含一些初始化数据"
   [:br]
   "例如"
   [:br]
   (pr-str
    [:div {:x-data-replicant-type :counter
           :x-data-replicant-initial-state (pr-str {:count 0})}
     "Counter"])
   [:br]
   "可以写一个 cljs main 函数, 发现所有有 data-replicant/type 的元素, 根据他的类型, 调用对应的渲染函数."
   [:br]
   [:div {:id "counter"
          :x-data-replicant-type :counter
          :x-data-replicant-initial-state (pr-str {:count 0})}
    "Counter"]
   [:script {:src "/js/progressive-enhancement.js"}]])
