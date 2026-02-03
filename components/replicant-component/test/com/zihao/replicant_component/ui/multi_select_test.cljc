(ns com.zihao.replicant-component.ui.multi-select-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.replicant-component.ui.multi-select :as multi-select]))

(deftest execute-action-toggle-selection-test
  (testing "Routes to toggle-selection action"
    (let [store (atom {})
          system {:store store}
          action :multi-select/toggle-selection
          args [:selections-path :option-value]
          result (multi-select/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          system {:store store}
          action :unknown/action
          args [:test-args]
          result (multi-select/execute-action system nil action args)]
      (is (nil? result)))))
