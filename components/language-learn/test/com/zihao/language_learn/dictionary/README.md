# Dictionary Core Tests

## Test File
`com.zihao.language_learn.dictionary.core-test`

## Running Tests

### Run all tests (unit + integration):
```bash
clj -M:test -e "(require 'com.zihao.language-learn.dictionary.core-test) (clojure.test/run-tests 'com.zihao.language-learn.dictionary.core-test)"
```

### Run only unit tests (from the component directory):
```bash
clj -M:test
# Then in the REPL:
(require 'com.zihao.language-learn.dictionary.core-test)
(clojure.test/run-tests 'com.zihao.language-learn.dictionary.core-test)
```

### Run a specific test:
```bash
clj -M:test -e "(do (require 'com.zihao.language-learn.dictionary.core-test) (clojure.test/run-test-var #'com.zihao.language-learn.dictionary.core-test/get-translations-multiple-words-test))"
```

## Test Coverage

### Unit Tests (7 tests)
1. **get-translations-multiple-words-test**: Tests retrieval of multiple translation words
2. **get-translations-single-word-test**: Tests retrieval of a single translation word
3. **get-translations-empty-test**: Tests handling of empty translation lists
4. **get-translations-trims-whitespace-test**: Tests that whitespace is trimmed from translation words
5. **get-translations-different-language-pairs-test**: Tests different language pair combinations
6. **get-translations-special-characters-test**: Tests handling of words with special characters (e.g., café, résumé)
7. **get-translations-order-preserved-test**: Tests that translation order is preserved

### Integration Tests (3 tests) - marked with ^:integration
1. **get-translations-integration-test**: Tests real API call for English to Spanish
2. **get-translations-integration-indonesian-english-test**: Tests real API call for Indonesian to English
3. **get-translations-integration-french-english-test**: Tests real API call for French to English

Note: Integration tests make actual HTTP requests to glosbe.com and may be slower or flaky due to network conditions.

## Results
- Total tests: 10
- Total assertions: 30
- Failures: 0
- Errors: 0

## Test Strategy

- **Mocking**: Unit tests use `with-redefs` to mock HTTP responses, ensuring tests are fast and deterministic
- **Edge Cases**: Tests cover empty results, single results, multiple results, special characters, and whitespace handling
- **Integration**: Integration tests verify actual API functionality (optional, can be skipped)
