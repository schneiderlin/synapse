(ns com.zihao.replicant-component.component.server-select-filter-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.replicant-component.component.server-select-filter :as server-select-filter]))

(deftest execute-action-server-filter-search-test
  (testing "Routes to server-filter-search action"
    (let [store (atom {:dispatch (fn [_store _event] nil)})
          action :server-filter-search
          args {:filter-key :test-filter
                :search-term "test"
                :query {:query/kind :test}
                :throttle-ms 500}
          result (server-select-filter/execute-action store nil action args)]
      (is (some? result)))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          action :unknown/action
          args [:test-args]
          result (server-select-filter/execute-action store nil action args)]
      (is (nil? result)))))
