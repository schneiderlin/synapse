网页自动化的时候, 需要用 selector 定位元素. 但是有一些网站的 accessability 做得不好, 很多的元素没有 id, 或者没有什么唯一的标识能定位到这个元素.
希望能自动生成 selector 规则. 好的 selector 应该有以下的 properties
- 稳定, 如果页面变更, 例如文案稍微修改, 加了一些 wrapper tag, 例如套多了一层 div 元素加了个边框, 页面小规模 rearrange 元素. 在页面变更的时候, 还能定位到之前的元素
- 精确, 只定位到目标元素, 没有其他的 false positive
<!-- - 解释性好, 生成出来是可读的, 当不可避免的要改动 selector 的时候, 容易人工修改 -->

chrome dev tool 可以复制 selector 和 xpath, 满足精确的条件, 但是不满足稳定, 一般是过度 specify 了. 
生成代码可以用 devtool 复制出来的作为基础, 不断的 relex, 直到 selector 刚好精确.

workflow 要怎么设置, 才能快速迭代. 
运行时先判断是不是唯一, 再对 locator 做下一步操作. 如果不是唯一, 在页面上 highlight 所有其他的选项.