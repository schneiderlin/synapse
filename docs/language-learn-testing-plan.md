# Language-Learn Component Testing Plan

## 概述

本文档描述了 `language-learn` 组件的测试策略。该组件基于 Replicant 架构，使用纯函数和 action-driven 的设计，使得大部分功能可以通过单元测试进行验证，而无需在浏览器中进行手动点击测试。

## 架构背景

根据 Replicant 架构设计：

- **View-function**: 输入 state 返回 hiccup 数据的函数
- **Action**: 纯数据，描述想要做什么，例如 `[[:store/assoc-in [prefix :input-text] :event/target.value]]`
- **Action-function**: 返回多个 actions 的函数，用于 hiccup 的 `:on` attribute
- **Action-handler**: 执行 effects 的函数，接收 system 和 interpolate 后的 action

由于这些组件都是纯函数或具有明确定义的副作用，我们可以通过模拟输入事件和 store 状态来进行单元测试。

## 测试策略

### 核心思路

**重要原则**: 不要测试 action 函数的输出（例如断言 `enter-article` 返回 `[:data/query ...]`）。这种测试是无用的，因为：

1. action 函数到 low-level actions 的转换是显而易见的
2. 这种转换不包含业务逻辑，不值得测试
3. 应该专注于测试真正的行为和状态变化

正确的测试方法是：

1. **创建模拟的 store**: 使用 `atom` 创建一个 store
2. **构建 execute-f**: 使用与实际代码相同的 `make-execute-f` 函数
3. **模拟用户输入**: 使用 test fixture 模拟 DOM 事件对象
4. **执行 actions**: 调用 `execute-f` 处理 actions
5. **断言最终状态**: 检查 store 的最终状态是否符合预期（这才是真正的行为测试）

### 测试流程示例

```clojure
;; 1. 创建模拟的 store
(def store (atom {}))

;; 2. 创建 execute-f（来自 replicant-main）
(def execute-f (rm/make-execute-f
                 (fn [system e action args]
                   ;; 注册 language-learn 的 action handler
                   (actions/execute-action system e action args))))

;; 3. 创建系统上下文
(def system {:store store
             :interpolate (rm/make-interpolate)
             :execute-actions execute-f})

;; 4. 模拟输入事件
(def input-event (js-obj "target" (js-obj "value" "Halo dunia")))

;; 5. 执行输入 action
(execute-f system input-event
          [[:store/assoc-in [prefix :input-text] :event/target.value]])

;; 6. 断言 store 状态
(is (= "Halo dunia" (get-in @store [prefix :input-text])))

;; 7. 模拟点击"Process Article"按钮
(def click-event (js-obj "type" "click"))
(execute-f system click-event
          [[:lingq/enter-article]])

;; 8. 断言 tokens 已生成（需要 mock 查询响应）
;; (is (seq (get-in @store [prefix :tokens])))
```

### 什么是无用的测试？

以下是一个**无用测试**的示例：

```clojure
;; ❌ 这是一个无用的测试！
(deftest enter-article-with-input-text
  (testing "Entering article with input-text initiates tokenization"
    (let [store (atom {prefix {:input-text "Halo dunia"}})
          result (actions/enter-article @store)]
      (is (vector? result))
      (is (= [[:data/query {:query/kind :query/tokenize-text
                            :query/data {:language "id"
                                         :text "Halo dunia"}}
               {:on-success [[:data/query {:query/kind :query/get-word-rating}
                              {:on-success [[:store/assoc-in [prefix :word->rating] :query/result]
                                            [:store/assoc-in [prefix :tokens] :query/result]]}]]}]]
             result)))))
```

**为什么这是无用的？**

1. **显而易见的转换**: action 函数 `enter-article` 将 high-level action 转换为 low-level actions（如 `[:data/query ...]`），这种转换是代码的直接映射，没有业务逻辑
2. **没有测试真正的行为**: 这个测试只验证了 action 函数的输出格式，没有测试执行这些 actions 后系统的状态
3. **维护负担**: 如果 action 格式稍有变化，测试就会失败，但功能可能完全正常

**正确的做法是**：

```clojure
;; ✅ 这是一个好的测试！
(deftest execute-action-click-unknown-word
  (testing "Action handler routes to click-unknown-word"
    (let [store (atom {})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]
      (execute-f system event [[:lingq/click-unknown-word {:word "anjing"}]])
      (let [state @store]
        (is (= "anjing" (get-in state [prefix :preview-word])))))))
```

这个测试实际调用了 `execute-action`，并验证了 store 的最终状态，这才是真正的行为测试。

## 测试范围

### 1. Action Handler 执行测试

**测试原则**: 对于 action handler，不要测试它们返回什么 actions，而是测试执行这些 actions 后 store 的最终状态。需要 mock 外部依赖（如查询、命令）的响应。

**为什么这样测试？**

- Action 函数只是将 high-level actions 转换为 low-level actions，这是实现细节
- 真正重要的是执行这些 actions 后，系统状态是否符合预期
- 测试执行过程可以验证整个 action pipeline 的正确性

#### 1.1 `click-unknown-word` Action

**测试场景**:
- 点击未知单词时，应该：
  - 设置 `preview-word` 为点击的单词
  - 设置 `preview-translation` 为 `nil`
  - 如果 mock 查询响应，可以测试 `preview-translation` 是否被正确更新

**示例测试**:
```clojure
(deftest execute-action-click-unknown-word
  (testing "Action handler routes to click-unknown-word and updates state"
    (let [store (atom {})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]
      (execute-f system event [[:lingq/click-unknown-word {:word "anjing"}]])
      (let [state @store]
        (is (= "anjing" (get-in state [prefix :preview-word])))))))
```

**注意**: 这个测试实际调用了 `execute-action`，并验证了 store 的最终状态。这才是有效的测试，因为它测试了实际的行为，而不是 action 的转换逻辑。

#### 1.2 `add-preview-word-to-database` Action

**测试场景**:
- 将预览单词添加到数据库时：
  - 调用 `command/add-new-word`
  - 成功后清空 `preview-word` 和 `preview-translation`
  - 刷新 word-rating 查询

#### 1.3 `clean-text` Action

**测试场景**:
- 清除文本时应该清空：
  - `tokens`
  - `selected-word`
  - `input-text`

#### 1.4 `enter-article` Action

**测试场景**:
- 处理文章时（需要 mock 查询响应）：
  - 初始化 store，设置 `input-text`
  - 执行 `enter-article` action
  - mock `tokenize-text` 查询返回 tokens
  - mock `get-word-rating` 查询返回 word-rating
  - 验证 store 中 `tokens` 和 `word->rating` 被正确更新

**示例测试**:
```clojure
(deftest execute-action-enter-article-with-mocked-queries
  (testing "enter-article with mocked query responses updates store correctly"
    (let [store (atom {prefix {:input-text "Halo dunia"}})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]

      ;; Mock query responses would go here
      ;; For example, using with-redefs or a mock handler

      (execute-f system event [[:lingq/enter-article]])

      ;; Assert final state
      (let [state @store]
        (is (seq (get-in state [prefix :tokens])))
        (is (map? (get-in state [prefix :word->rating])))))))
```

**重要**: 不要测试 `enter-article` 返回什么 actions，要测试执行后的最终状态。

### 2. View Function 测试

**测试原则**: 对于 view function，测试它们基于给定状态返回的 hiccup 数据是否正确。

**与 action handler 测试的区别**:
- View functions 是纯函数（state → hiccup），没有副作用
- 可以直接调用函数并断言返回值
- 重点测试：不同状态下的渲染结果是否正确

#### 2.1 `textarea-ui` View Function

**测试场景**:
- 不显示 tokens 时，显示 textarea
- textarea 的 `:on :input` action 应该更新 `input-text`
- 清除按钮应该调用 `:lingq/clean-text`

#### 2.2 `article-ui` View Function

**测试场景**:
- 渲染 tokens 时每个 token 应该有正确的 class
- 未知单词应该有可点击的 cursor-pointer 样式
- 已知单词不应该有背景色
- 点击未知单词触发 `:lingq/click-unknown-word`
- 点击已知单词更新 `selected-word`
- 空格和标点符号不应该包装在 span 中

#### 2.3 `word-rating-ui` View Function

**测试场景**:
- 显示当前选中的单词
- 显示当前的 rating
- 提供 1-5 的评分按钮
- 点击评分按钮触发 `:command/update-word-rating`

#### 2.4 `main` View Function

**测试场景**:
- 有 tokens 时显示 article-ui
- 没有 tokens 时显示 textarea-ui
- 有 `preview-word` 时显示预览面板
- 有 `selected-word` 且没有 `preview-word` 时显示评分面板

### 3. 集成测试

#### 3.1 完整的文章输入流程

**测试场景**:
1. 用户输入文章文本
2. store 的 `input-text` 被更新
3. 用户点击"Process Article"按钮
4. `enter-article` action 被触发
5. tokens 被生成并存储
6. view 从 textarea 切换到 article

**示例测试**:
```clojure
(deftest complete-article-input-flow
  (testing "Complete flow from text input to article display"
    (let [store (atom {})
          execute-f (rm/make-execute-f
                      (fn [system e action args]
                        (actions/execute-action system e action args)))
          system {:store store
                  :execute-actions execute-f
                  :interpolate (rm/make-interpolate)}]

      ;; 模拟输入
      (let [input-event (js-obj "target" (js-obj "value" "Halo dunia"))]
        (execute-f system input-event
                  [[:store/assoc-in [prefix :input-text] :event/target.value]]))

      (is (= "Halo dunia" (get-in @store [prefix :input-text])))

      ;; 模拟点击处理按钮（需要 mock query 响应）
      (let [click-event (js-obj)]
        (execute-f system click-event
                  [[:lingq/enter-article]]))

      ;; 验证最终状态：tokens 已生成
      ;; 注意：实际测试时需要 mock 后端查询响应
      ;; 可以使用 with-redefs 或者 mock handler
      ;; (is (seq (get-in @store [prefix :tokens])))
      )))
```

#### 3.2 单词学习流程

**测试场景**:
1. 用户点击未知单词
2. 显示单词预览和翻译
3. 用户点击"添加到数据库"
4. 单词被添加，预览清空
5. 再次点击相同单词时显示评分面板

### 4. Edge Cases 测试

#### 4.1 空输入处理

**测试场景**:
- 输入空字符串
- 输入只有空格
- 输入特殊字符

#### 4.2 非字符串 token 处理

**测试场景**:
- API 返回非字符串 token 时的处理
- 确保不会抛出 `toLowerCase is not a function` 错误

#### 4.3 并发操作

**测试场景**:
- 快速连续点击多个未知单词
- 在翻译查询完成前点击清除文本

## 测试工具和 Fixture

### Event Fixtures

```clojure
(defn make-input-event [value]
  (js-obj "target" (js-obj "value" value)))

(defn make-click-event []
  (js-obj "type" "click" "target" (js-obj)))

(defn make-checkbox-event [checked]
  (js-obj "target" (js-obj "checked" checked)))
```

### System Setup Helper

```clojure
(defn make-test-system
  "Create a test system with mock store and execute-f"
  []
  (let [store (atom {})
        execute-f (rm/make-execute-f
                    (fn [system e action args]
                      (actions/execute-action system e action args)))]
    {:store store
     :execute-actions execute-f
     :interpolate (rm/make-interpolate)}))
```

### Mock Query/Command Handler

```clojure
(defn mock-query-handler
  "Mock query handler that returns predefined results"
  [responses]
  (fn [system e action args]
    (let [query (second args)
          on-success (:on-success (nth args 2 nil))
          response (get responses (:query/kind query))]
      (when (and response on-success)
        (let [actions (on-success response)]
          ((:execute-actions system) e actions)))))
```

## 实现计划

### Phase 1: Action Handler 单元测试

- [ ] `click-unknown-word` 测试（测试执行后的状态变化）
- [ ] `add-preview-word-to-database` 测试（测试执行后的状态变化）
- [ ] `clean-text` 测试（测试执行后的状态变化）
- [ ] `enter-article` 测试（需要 mock 查询，测试执行后的状态变化）

**注意**: 所有测试都应该通过 `execute-f` 执行 actions，并验证 store 的最终状态，而不是直接调用 action 函数并断言返回的 actions。

### Phase 2: View Function 单元测试

- [ ] `textarea-ui` 测试
- [ ] `article-ui` 测试
- [ ] `word-rating-ui` 测试
- [ ] `main` 测试

### Phase 3: 集成测试

- [ ] 完整的文章输入流程
- [ ] 单词学习流程
- [ ] Edge cases 测试

## 测试运行

### 运行特定测试文件

```bash
# 运行 lingq actions 测试
clj -M:test -e "(require 'com.zihao.language-learn.lingq.actions-test) (clojure.test/run-tests 'com.zihao.language-learn.lingq.actions-test)"

# 运行 lingq article 测试（需要创建）
clj -M:test -e "(require 'com.zihao.language-learn.lingq.article_test) (clojure.test/run-tests 'com.zihao.language-learn.lingq.article_test)"
```

### 从组件目录运行

```bash
cd components/language-learn
clj -M:test
```

## 参考

- [Replicant Architecture](../.claude/skills/architecture/SKILL.md)
- [Clojure Testing Documentation](https://clojure.org/guides/test)
- [Existing Tests](../components/language-learn/test/com/zihao/language_learn/lingq/actions_test.clj)

## 注意事项

1. **Mock External Dependencies**: 查询和命令需要 mock 后端响应，因为单元测试不应该依赖真实的数据库或 API

2. **Isolate Tests**: 每个测试应该独立运行，不依赖其他测试的状态

3. **Test Naming**: 使用描述性的测试名称，明确说明测试的场景

4. **Fixture Reuse**: 创建可重用的 fixture 函数来减少代码重复

5. **Avoid Browser Testing**: 由于我们测试的是纯函数和 action handlers，不需要启动浏览器或使用工具如 Selenium

6. **CLJC Files**: 注意 `.cljc` 文件中的平台特定代码，测试时可能需要条件编译

7. **不要测试 Action 转换**: ❌ 不要直接调用 action 函数并断言返回的 actions（例如断言 `enter-article` 返回 `[:data/query ...]`）。这些转换是显而易见的，不包含业务逻辑。✅ 应该使用 `execute-f` 执行 actions，并验证 store 的最终状态。

8. **测试行为而非实现**: 关注"系统做了什么"（行为），而不是"系统如何做"（实现细节）。状态断言是测试行为的最佳方式。

## 持续改进

随着功能的增加，测试计划应该持续更新：

1. 新增的 action handler 需要对应的单元测试
2. 新增的 view function 需要测试其渲染结果
3. 集成测试应该覆盖主要用户流程
4. 定期 review 测试覆盖率，确保核心功能都有测试覆盖
