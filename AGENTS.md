# test
- clj -M:test -e "(require 'namespace.test) (clojure.test/run-tests 'namespace.test)", to test a specific function
- Or from a component directory "clj -M:test"

# lint
`clj-kondo --lint components` or `clj-kondo --lint bases`
or just one components `clj-kondo --lint components/language-learn`
