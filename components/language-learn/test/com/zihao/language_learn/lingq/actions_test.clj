(ns com.zihao.language-learn.lingq.actions-test
  (:require [clojure.test :refer :all]
            [com.zihao.language-learn.lingq.actions :as actions]))

(deftest execute-action-click-unknown-word-test
  (testing "Action handler routes to click-unknown-word"
    (let [store (atom {})
          event nil
          action :lingq/click-unknown-word
          args {:word "anjing"}
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-clean-text-test
  (testing "Action handler routes to clean-text"
    (let [store (atom {})
          event nil
          action :lingq/clean-text
          args []
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-enter-article-test
  (testing "Action handler routes to enter-article"
    (let [store (atom {})
          event nil
          action :lingq/enter-article
          args {:article "Halo dunia"}
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-unknown-test
  (testing "Action handler returns nil for unknown action"
    (let [store (atom {})
          event nil
          action :lingq/unknown-action
          args []
          result (actions/execute-action {:store store} event action args)]
      (is (nil? result)))))
