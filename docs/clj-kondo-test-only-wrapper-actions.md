# Clj-Kondo Linter: Detect Test-Only Action Functions

## Problem

We want to discourage creating action functions that are used only in test files and never in production code. Tests should exercise production-used action functions, not action functions created solely for testing.

Example of what we want to prevent:

```clojure
;; In C:\Users\zihao\Desktop\workspace\opensource\synapse\components\language-learn\src\com\zihao\language_learn\actions.cljs
(defn lingq/select-word
  [state [word translation]]
  [[:assoc-in [:lingq :selected-word] word]])

;; This action function is only used in test files like ui-state-transition-show-rating.
;; Tests should exercise production-used action functions, not test-only wrapper functions.
```

The rationale: Tests should exercise production-used actions. Creating action functions solely for testing defeats this purpose - tests should test actual production code paths.

## Detection Criteria

An action should be flagged if it is **only used in test files** and **never** in production code.

**Note**: It doesn't matter if the action contains only primitive operations or has complex logic. The only criterion is test-only usage.

- Test files: files in `C:\Users\zihao\Desktop\workspace\opensource\synapse\*\test\*` directories or ending in `*_test.clj`, `test_*.clj`
- Production files: files in `C:\Users\zihao\Desktop\workspace\opensource\synapse\*\src\*` directories (non-test source files)

## Implementation Requirements

The linter should:

1. **Find all action functions**
   - Look for `defn` definitions that return action vectors/lists (vectors containing other vectors where each inner vector starts with a keyword)
   - Focus on functions in files named `actions*.cljs` or `actions*.clj`
   - Extract the function name

2. **Build usage map**
   - Scan all files for references to each action function
   - Track which files (test vs production) use each action function

3. **Report violations**
   - Action functions that are only used in test files (not used in any production file)
   - Suggest:
     - "Use this action function in production code"

## Example Output

```
Warning: Test-only action function detected

File: C:\Users\zihao\Desktop\workspace\opensource\synapse\components\language-learn\src\com\zihao\language_learn\actions.cljs:42
Action function: lingq/select-word

This action function is only used in test files and never used in production code.

Tests should exercise production-used actions, not action functions created solely for testing.

Consider:
- Use this action function in production code, OR
- Remove this action function and use an existing production action function in your test

Found in tests:
- C:\Users\zihao\Desktop\workspace\opensource\synapse\components\language-learn\test\com\zihao\language_learn\ui_state_transition_show_rating_test.clj:23
```

## Notes

- The linter should be configurable (e.g., allowlist of action functions that are intentionally test-only)
- Performance: analyze efficiently, avoid re-parsing files multiple times
- Focus on `defn` definitions that return action vectors/lists (vectors of vectors starting with keywords)
- Action complexity (number of primitive actions, logic, etc.) is NOT a criterion for violation
- An "action" is defined as a vector like `[:assoc-in [:path] value]` where the first element is a keyword
- Tests should NOT use primitive actions directly - they should exercise production-used action functions
