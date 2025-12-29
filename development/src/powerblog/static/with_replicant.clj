(ns powerblog.static.with-replicant)

(defn counter [])

(def page
  [:div
   [:p "由于 replicant 只是一个 render library, 主要只是把 hiccup 变成 html. 所以也可以在后端使用, 只在 statis site generator 的 build phase 使用."]
   [:p "当前这个页面就是由 replicant 渲染的. 如何渲染 stateful 的组件? replicant 需要 watch 一个 atom, 根据 atom 变化重新渲染组件. 因此需要部分 JavaScript 代码, 创建 atom 和 watch"]
   [:p "后端渲染的 html, 只提供 attribute 标记, 表明这个 element 需要前端接管. 并且可能包含一些初始化数据"]
   #_[:div {:data-replicant/type :counter
          :data-replicant/initial-state {:count 0}}
    "Counter"]])