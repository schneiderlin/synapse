(ns com.zihao.replicant-component.component.table-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.replicant-component.component.table :as table]))

(deftest execute-action-clear-selected-ids-test
  (testing "Routes to clear-selected-ids action"
    (let [store (atom {})
          system {:store store}
          action :table/clear-selected-ids
          args [:test-table-id]
          result (table/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-select-row-test
  (testing "Routes to select-row action"
    (let [store (atom {})
          system {:store store}
          action :table/select-row
          args {:row {:id 1} :selected? true :table-id :test-table}
          result (table/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-toggle-all-test
  (testing "Routes to toggle-all action"
    (let [store (atom {})
          system {:store store}
          action :table/toggle-all
          args {:table-id :test-table :rows [{:id 1} {:id 2}] :selected? true}
          result (table/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          system {:store store}
          action :unknown/action
          args [:test-args]
          result (table/execute-action system nil action args)]
      (is (nil? result)))))
