# Testing UI with Pure Functions and AI-Friendly Error Suggestions

This document describes how we test UI in the Replicant-based stack by treating the view as a pure function (state → hiccup), and how the hiccup test helper’s error messages are designed to guide an AI agent (or a human) to fix failing tests quickly.

## 1. Leveraging Pure Functions to Test UI

### View as a pure function

In our architecture, the UI is produced by **view functions**: they take application state and return a **hiccup** tree (data), not DOM or pixels. For example:

```clojure
(render/main state)  ; => [:div ...]  (hiccup data)
```

Because this is a pure function:

- **No browser required.** We call `render/main` with a given `state` and assert on the returned data.
- **Deterministic.** Same `state` always yields the same hiccup.
- **Easy to test.** We control the only input (`state`) and assert on the only output (hiccup).

So “UI tests” are just unit tests: call the view with state, then check the hiccup for the right structure, tags, and content.

### What we assert on

We do **not**:

- Start a browser or use Selenium/Playwright.
- Assert that “something” was returned (e.g. `(is (vector? result))`).
- Assert on the exact shape of the whole tree (brittle and unnecessary).

We **do**:

- Call the view with a specific `state`.
- Use helpers to ask: “does this hiccup contain this tag / this text / these attributes?”
- Assert presence or absence of the elements that matter for the behavior we care about.

That way we test **behavior** without tying tests to low-level structure.

### Test flow in practice

1. **Build state**  
   Use an `atom` as the store and set the slice of state that the view depends on (e.g. `prefix` keys for the language-learn UI).

2. **Render**  
   Call the view: `(render/main @store)` (or the appropriate view for the component).

3. **Assert on hiccup**  
   Use the test helper macros (see below) to assert that the hiccup contains (or does not contain) certain elements or text.

4. **State transitions (integration-style)**  
   To test “user did X, then UI shows Y”:
   - Start from an initial state and assert the first UI (e.g. textarea).
   - Run the same **action pipeline** the app uses (e.g. `execute-f` with `[:lingq/set-tokens ...]` or other actions).
   - Re-read state from the store, call `render/main` again, and assert the new UI (e.g. “Clear Article” and no textarea).

So we get “integration” coverage (state → actions → new state → new UI) while still only calling pure view functions and action handlers; no DOM or browser is involved.

### Example (from language-learn integration tests)

```clojure
(deftest ui-state-transition-textarea-to-article
  (testing "UI state transition: textarea to article"
    (let [store (atom {})
          execute-f (apply rm/make-execute-f [actions/execute-action])
          system {:store store}]

      ;; Initial state: no tokens → textarea UI
      (let [result (render/main @store)]
        (is-in-hiccup result :textarea)
        (is-in-hiccup result "Process Article")
        (is-not-in-hiccup result "Clear Article"))

      ;; After “enter article”: tokens present → article UI
      (execute-f system nil [[:lingq/set-tokens {:tokens ["Halo" " " "dunia"]}]])
      (let [result (render/main @store)]
        (is-in-hiccup result "Clear Article")
        (is-not-in-hiccup result :textarea)))))
```

Summary:

- **Pure view:** `render/main` is only called with state; we never touch the DOM.
- **State transitions:** We drive the same `execute-f` the app uses, then re-render and assert again.
- **Assertions:** We use `is-in-hiccup` / `is-not-in-hiccup` so tests stay readable and stable.

---

## 2. Error suggestions in the test helper to guide the AI agent

When a content assertion fails, the test helper can append a **suggestion** to the failure message. That helps both humans and AI agents fix the test without guessing.

### Macros: `is-in-hiccup` and `is-not-in-hiccup`

The helper (`components/replicant-main/test/com/zihao/replicant_main/hiccup_test_helper.cljc`) exposes two macros:

- **`is-in-hiccup hiccup needle`**  
  Asserts that `needle` appears in `hiccup`:
  - If `needle` is a **keyword** (e.g. `:textarea`), it checks for an element with that tag.
  - If `needle` is a **string** (e.g. `"Process Article"`), it checks for a node whose content equals that string (by default, exact match).

- **`is-not-in-hiccup hiccup needle`**  
  Asserts that `needle` does **not** appear in `hiccup` (same tag vs content rules).

So tests stay short and intent is clear: “this hiccup must (or must not) contain this tag or this text.”

### Why the failure message matters for an AI agent

If the assertion only said `"Process Article" is not in hiccup`, an agent might:

- Change the expected string arbitrarily,
- Or give up.

It would not know that the **actual** hiccup might contain the same text with different formatting (e.g. extra spaces) or case. So we add **targeted suggestions** when we detect a near-match.

### How the suggestion is generated

When a **content** (string) assertion fails, the helper:

1. Tries an **exact** match (already failed).
2. Tries **`:partial true`**: content appears as a substring in some string node.
3. Tries **`:case-insensitive true`**: same string, ignoring case.

If either the partial or the case-insensitive check **would** have passed, the failure message is extended with a hint, for example:

- `"译文:" is not in hiccup (maybe try :partial true?)`
- `"word preview" is not in hiccup (maybe try :case-insensitive true?)`
- `"foo" is not in hiccup (maybe try :partial true or :case-insensitive true?)`

So the message tells the agent (or developer) **exactly** which option to try, instead of leaving them to guess.

### Implementation detail: `content-match-suggestion`

The logic lives in `content-match-suggestion`: after an exact match fails, it calls `contains-element-with-content?` with `:partial true` and with `:case-insensitive true`. If either matches, it returns the appropriate “maybe try …” string, which the macro appends to the base message. So the suggestion is only shown when it is actually applicable.

### Using the suggestion in tests

Normal usage stays simple; you only add options when you intentionally want partial or case-insensitive matching:

```clojure
;; Exact match (default); on failure, message may suggest :partial or :case-insensitive
(is-in-hiccup result "Process Article")

;; If you really want substring or case-insensitive match, use the low-level helper:
(is (contains-element-with-content? result "process article" :case-insensitive true))
```

So:

- **Default:** exact match; failure message can suggest `:partial true` or `:case-insensitive true` when relevant.
- **AI (or human) fixing a failing test:** Read the suggestion and either adjust the expected string (e.g. fix typo or spacing) or use `contains-element-with-content?` with the suggested option.

### Summary

- **Pure functions:** View is state → hiccup; we test UI by calling the view with state and asserting on hiccup, with no browser.
- **Stable, behavior-focused assertions:** We use `is-in-hiccup` / `is-not-in-hiccup` (and optionally `contains-element?`, `contains-element-with-attr?`, etc.) to check only what matters.
- **AI-friendly failures:** When a content assertion fails, the helper appends a concrete suggestion (“maybe try :partial true?” or “:case-insensitive true?”) so an AI agent can fix the test by following the hint instead of guessing.

