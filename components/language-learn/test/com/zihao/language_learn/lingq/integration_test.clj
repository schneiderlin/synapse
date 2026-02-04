(ns com.zihao.language-learn.lingq.integration-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.language-learn.lingq.render :as render]
            [com.zihao.language-learn.lingq.common :refer [prefix]]
            [com.zihao.language-learn.lingq.actions :as actions]
            [com.zihao.replicant-main.interface :as rm]
            [com.zihao.replicant-main.hiccup-test-helper :refer [is-in-hiccup is-not-in-hiccup]]))

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

(deftest ui-state-transition-textarea-to-article
  (testing "UI state transition: textarea to article"
    (let [store (atom {})
          system {:store store}
          execute-f (apply rm/make-execute-f [actions/execute-action])]

      ;; Initial state: no tokens (textarea shown)
      (let [state @store
            result (render/main state)]
        ;; Should contain textarea element
        (is-in-hiccup result :textarea)
        ;; Should contain "Process Article" button
        (is-in-hiccup result "Process Article")
        ;; Should NOT contain "Clear Article" button
        (is-not-in-hiccup result "Clear Article"))

      ;; After enter-article: tokens present (article shown) - use higher-level action
      (execute-f system nil [[:lingq/set-tokens {:tokens ["Halo" " " "dunia"]}]])
      (let [state @store
            result (render/main state)]
        ;; Should contain "Clear Article" button
        (is-in-hiccup result "Clear Article")
        ;; Should NOT contain textarea element (article view is shown instead)
        (is-not-in-hiccup result :textarea)))))


(deftest ui-state-transition-show-preview
  (testing "UI state transition: Show preview panel"
    (let [store (atom {})
          system {:store store}
          execute-f (apply rm/make-execute-f [actions/execute-action])]

      ;; Initial state: no preview (preview panel not shown)
      (let [state @store
            result (render/main state)]
        ;; Should NOT contain preview panel elements
        (is-not-in-hiccup result "Word Preview")
        (is-not-in-hiccup result "原文:")
        (is-not-in-hiccup result "译文:"))

      ;; Click unknown word: preview panel shown - use higher-level action
      (execute-f system nil [[:lingq/set-preview {:word "anjing" :translation ["calm" "peaceful"]}]])
      (let [state @store
            result (render/main state)]
        ;; Should contain preview panel header
        (is-in-hiccup result "Word Preview")
        ;; Should contain the preview word
        (is-in-hiccup result "anjing")
        ;; Should contain translations (note: the translations are joined with ", " when rendered)
        (is-in-hiccup result "原文: ")
        (is-in-hiccup result "译文: ")
        (is-in-hiccup result "calm, peaceful")
        ;; Should contain "添加到数据库" button
        (is-in-hiccup result "添加到数据库")))))


(deftest ui-state-transition-show-rating
  (testing "UI state transition: Show rating panel"
    (let [store (atom {})
          system {:store store}
          execute-f (apply rm/make-execute-f [actions/execute-action])]

      ;; Initial state: no selection (rating panel not shown)
      (let [state @store
            result (render/main state)]
        ;; Should NOT contain rating buttons
        (is-not-in-hiccup result "again")
        (is-not-in-hiccup result "hard")
        (is-not-in-hiccup result "good")
        (is-not-in-hiccup result "easy"))

      ;; Click known word: rating panel shown - use higher-level actions
      (execute-f system nil [[:lingq/set-tokens {:tokens ["anjing"]}]
                             [:lingq/select-word {:word "anjing"}]])
      (let [state @store
            result (render/main state)]
        ;; Should contain all 4 rating buttons
        (is-in-hiccup result "again")
        (is-in-hiccup result "hard")
        (is-in-hiccup result "good")
        (is-in-hiccup result "easy")))))
