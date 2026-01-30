---
description: Test action handler execute-action for a component
---

Write unit tests for the execute-action handler in the component: $ARGUMENTS

Steps:
1. Find the actions.cljc or actions.clj file for the component $ARGUMENTS in components/$ARGUMENTS/src/
2. Read the file to understand the execute-action function signature
3. Create or extend the test file at components/$ARGUMENTS/test/.../actions_test.cljc or .clj
4. Write tests for all action routes in execute-action using this pattern:
   - Test each action routing (e.g., :component-name/action-name)
   - Test unknown action returns nil
   - Use minimal test setup (empty atom for store, nil for event)
   - Follow the existing test patterns from test-coverage-plan.md

Testing approach:
- Focus on testing execute-action handler routing logic
- Don't test individual action functions (they are static configuration)
- Use happy path tests only
- Follow the test file structure: ns, require clojure.test and actions namespace

After writing tests, run them to verify they pass using the command:
clj -M:test -e "(require 'namespace.test) (clojure.test/run-tests 'namespace.test)"
