(ns com.zihao.language-learn.lingq.actions-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.replicant-main.interface :as rm]
            [com.zihao.language-learn.lingq.actions :as actions]
            [com.zihao.language-learn.lingq.common :refer [prefix]]))

;; =============================================================================
;; execute-action Tests
;; =============================================================================
(deftest execute-action-click-unknown-word
  (testing "Action handler routes to click-unknown-word and updates store state"
    (let [store (atom {})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]
      (execute-f system event [[:lingq/click-unknown-word {:word "anjing"}]])
      (let [state @store]
        (is (= "anjing" (get-in state [prefix :preview-word])))))))

(deftest execute-action-clean-text
  (testing "Action handler routes to clean-text and clears store state"
    (let [store (atom {prefix {:tokens ["test"]
                                :selected-word "test"
                                :input-text "test text"}})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]
      (execute-f system event [[:lingq/clean-text]])
      (let [state @store]
        (is (= [] (get-in state [prefix :tokens])))
        (is (nil? (get-in state [prefix :selected-word])))
        (is (nil? (get-in state [prefix :input-text])))))))

(deftest execute-action-unknown
  (testing "Action handler returns nil for unknown action"
    (let [store (atom {})
          result (actions/execute-action {:store store} nil :lingq/unknown-action [])]
      (is (nil? result)))))
