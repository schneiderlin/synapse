# test
`clj-nrepl-eval -p NREPL_PORT "(require '[clojure.test :as t]) (r/run-test TEST_VAR)"`

after change the test code. reload file to take effect.
`clj-nrepl-eval -p NREPL_PORT "(load-file FILE_PATH.clj)"`

# lint
`clj-kondo --lint components` or `clj-kondo --lint bases`
or just one components `clj-kondo --lint components/language-learn`
