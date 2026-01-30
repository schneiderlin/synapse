(ns com.zihao.replicant-component.ui.multi-input-test
  (:require [clojure.test :refer :all]
            [com.zihao.replicant-component.ui.multi-input :as multi-input]))

(deftest execute-action-paste-value-test
  (testing "Routes to paste-value action"
    (let [store (atom {})
          system {:store store}
          action :multi-input/paste-value
          args [:items-path "test\nvalue"]
          result (multi-input/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          system {:store store}
          action :unknown/action
          args [:test-args]
          result (multi-input/execute-action system nil action args)]
      (is (nil? result)))))
