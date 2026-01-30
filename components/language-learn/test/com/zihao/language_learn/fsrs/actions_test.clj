(ns com.zihao.language-learn.fsrs.actions-test
  (:require [clojure.test :refer :all]
            [com.zihao.language-learn.fsrs.actions :as actions]))

(deftest execute-action-load-due-cards-test
  (testing "Action handler routes to load-due-cards"
    (let [store (atom {})
          event nil
          action :card/load-due-cards
          args []
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-repeat-card-test
  (testing "Action handler routes to repeat-card"
    (let [store (atom {})
          event nil
          action :card/repeat-card
          args {:id 1 :rating :good}
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-show-answer-test
  (testing "Action handler routes to show-answer"
    (let [store (atom {})
          event nil
          action :flashcard/show-answer
          args []
          result (actions/execute-action {:store store} event action args)]
      (is (vector? result)))))

(deftest execute-action-unknown-test
  (testing "Action handler returns nil for unknown action"
    (let [store (atom {})
          event nil
          action :card/unknown-action
          args []
          result (actions/execute-action {:store store} event action args)]
      (is (nil? result)))))
