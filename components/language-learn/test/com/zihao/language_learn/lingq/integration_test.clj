(ns com.zihao.language-learn.lingq.integration-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.replicant-main.interface :as rm]
            [com.zihao.language-learn.lingq.actions :as actions]
            [com.zihao.language-learn.lingq.render :as render]
            [com.zihao.language-learn.lingq.article :as article]
            [com.zihao.language-learn.lingq.common :refer [prefix]]))

;; =============================================================================
;; Complete Article Input Flow Tests
;; =============================================================================

;; TODO: Add integration tests with mocked queries
;; These tests should simulate the complete flow from user action to store state changes
;; Example:
;; (deftest complete-article-input-flow
;;   (testing "Complete flow from text input to article display"
;;     (let [store (atom {})
;;           system {:store store}
;;           execute-f (apply rm/make-execute-f [actions/execute-action])]
;;       ;; Set up initial text
;;       (swap! store assoc-in [prefix :input-text] "Halo dunia")
;;
;;       ;; Execute enter-article action (requires mocked query responses)
;;       ;; (execute-f system nil [[:lingq/enter-article]])
;;
;;       ;; Verify tokens are generated
;;       ;; (is (seq (get-in @store [prefix :tokens])))
;;       )))

;; =============================================================================
;; UI State Transitions Tests
;; =============================================================================

;; TODO: 这也不是好的 test, 不应该直接 swap store, 应该通过 execute-f
(deftest ui-state-transition-textarea-to-article
  (testing "UI state transition: textarea to article"
    (let [store (atom {})]

      ;; Initial state: no tokens (textarea shown)
      (let [state @store
            result (render/main state)]
        (is (vector? result)))

      ;; After enter-article: tokens present (article shown)
      (swap! store assoc-in [prefix :tokens] ["Halo" " " "dunia"])
      (let [state @store
            result (render/main state)]
        (is (vector? result))))))


(deftest ui-state-transition-show-preview
  (testing "UI state transition: Show preview panel"
    (let [store (atom {})]
      ;; Initial state
      (let [state @store
            result (render/main state)]
        (is (vector? result)))

      ;; Click unknown word
      (swap! store assoc-in [prefix :preview-word] "test")
      (swap! store assoc-in [prefix :preview-translation] ["translation"])
      (let [state @store
            result (render/main state)]
        (is (vector? result))))))


(deftest ui-state-transition-show-rating
  (testing "UI state transition: Show rating panel"
    (let [store (atom {})]
      ;; Click known word (with tokens and selected-word)
      (swap! store assoc-in [prefix :tokens] ["test"])
      (swap! store assoc-in [prefix :selected-word] "test")
      (let [state @store
            result (render/main state)]
        (is (vector? result))))))
