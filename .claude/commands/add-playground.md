---
description: 添加新的 component, 用于测试和eval新的library
---

首先讯问用户需要测试哪个 library, 假设用户说 stasis.
那么 component 的名字就是 playground-stasis.

通过 `clojure -M:poly create component name:playground-<libname>` 创建一个 component 的 scaffold 在 components 文件夹中.
并且在根目录的 ./deps.edn 中添加新创建的 playground, 放在其他 playground 的旁边. 例如
```edn
poly/playground-jsonrpc {:local/root "components/playground-jsonrpc"}
poly/playground-rama {:local/root "components/playground-rama"}
```

然后修改 components/playground-<libname> 里面的 deps.edn, 添加依赖
