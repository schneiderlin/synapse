(ns com.zihao.replicant-component.component.completion-input-test
  (:require [clojure.test :refer :all]
            [com.zihao.replicant-component.component.completion-input :as completion-input]))

(deftest execute-action-input-change-test
  (testing "Routes to input-change action"
    (let [store (atom {})
          system {:store store}
          action :completion-input/input-change
          args [:input-path :options-path "test value"]
          result (completion-input/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          system {:store store}
          action :unknown/action
          args [:test-args]
          result (completion-input/execute-action system nil action args)]
      (is (nil? result)))))
