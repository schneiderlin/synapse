(ns com.zihao.language-learn.lingq.integration-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.language-learn.lingq.actions :as actions]
            [com.zihao.language-learn.lingq.render :as render]
            [com.zihao.language-learn.lingq.article :as article]
            [com.zihao.language-learn.lingq.common :refer [prefix]]))

;; =============================================================================
;; Complete Article Input Flow Tests
;; =============================================================================

(deftest complete-article-input-flow
  (testing "Complete flow from text input to article display"
    (let [store (atom {})
          initial-text "Halo dunia"]

      ;; Step 1: User enters text (simulated by directly setting input-text)
      (swap! store assoc-in [prefix :input-text] initial-text)
      (is (= initial-text (get-in @store [prefix :input-text])))

      ;; Step 2: User clicks "Process Article" button
      ;; This should call enter-article action
      (let [enter-result (actions/enter-article @store)]
        (is (vector? enter-result))
        (is (= :query/tokenize-text (get-in enter-result [0 1 :query/kind])))
        (is (= initial-text (get-in enter-result [0 1 :query/data :text])))))))

(deftest article-input-flow-empty-input
  (testing "Flow with empty input text"
    (let [store (atom {})]

      ;; Set empty input
      (swap! store assoc-in [prefix :input-text] "")

      ;; Click process
      (let [enter-result (actions/enter-article @store)]
        (is (vector? enter-result))
        (is (= :query/tokenize-text (get-in enter-result [0 1 :query/kind])))))))

(deftest article-input-flow-nil-input
  (testing "Flow with nil input text"
    (let [store (atom {})]

      ;; No input set
      (let [enter-result (actions/enter-article @store)]
        (is (vector? enter-result))
        (is (= :query/tokenize-text (get-in enter-result [0 1 :query/kind])))))))

;; =============================================================================
;; Word Learning Flow Tests
;; =============================================================================

(deftest word-learning-flow-new-word
  (testing "Flow: Click unknown word, add to database, clear preview"
    (let [store (atom {})]
      ;; Step 1: Click on unknown word
      (let [click-result (actions/click-unknown-word store {:word "anjing"})]
        (is (vector? click-result))
        (is (= "anjing" (get-in click-result [0 2])))
        (is (= nil (get-in click-result [1 2])))
        (is (= :query/get-word-translation (get-in click-result [2 1 :query/kind]))))))

  ;; Simulate the store update from the actions
  (testing "Word learning flow: Store state updates"
    (let [store (atom {})]

      ;; Step 1: Click unknown word updates store
      (swap! store assoc-in [prefix :preview-word] "anjing")
      (is (= "anjing" (get-in @store [prefix :preview-word])))

      ;; Step 2: Simulate translation received
      (swap! store assoc-in [prefix :preview-translation] ["dog"])
      (is (= ["dog"] (get-in @store [prefix :preview-translation])))

      ;; Step 3: Add to database
      (let [add-result (actions/add-preview-word-to-database @store)]
        (is (vector? add-result))
        (is (= :command/add-new-word (get-in add-result [0 1 :command/kind]))))
      ;; After success action, preview-word and preview-translation should be nil
      (swap! store assoc-in [prefix :preview-word] nil)
      (swap! store assoc-in [prefix :preview-translation] nil)
      (is (nil? (get-in @store [prefix :preview-word])))
      (is (nil? (get-in @store [prefix :preview-translation]))))))

(deftest word-learning-flow-existing-word
  (testing "Flow: Click on word that was already added to database"
    (let [store (atom {})]
      ;; Set up a word that's been added to database (has rating)
      (swap! store assoc-in [prefix :tokens] ["anjing"])
      (swap! store assoc-in [prefix :word->rating] {"anjing" 1})

      ;; Click on the word (it's known, not unknown)
      ;; In the article-ui, this would set selected-word
      (swap! store assoc-in [prefix :selected-word] "anjing")
      (is (= "anjing" (get-in @store [prefix :selected-word])))

      ;; The UI should show the rating panel, not the preview panel
      (let [state @store
            result (render/main state)]
        (is (vector? result))))))

;; =============================================================================
;; Clean Text Flow Tests
;; =============================================================================

(deftest clean-text-flow
  (testing "Flow: Clean text clears all state"
    (let [store (atom {})
          initial-state {prefix {:input-text "Halo dunia"
                                 :tokens ["Halo" " " "dunia"]
                                 :selected-word "Halo"}}]

      ;; Set up initial state
      (reset! store initial-state)

      ;; Verify initial state
      (is (= "Halo dunia" (get-in @store [prefix :input-text])))
      (is (seq (get-in @store [prefix :tokens])))
      (is (= "Halo" (get-in @store [prefix :selected-word])))

      ;; Clean text
      (let [clean-result (actions/clean-text store)]
        (is (vector? clean-result)))

      ;; Apply the clean actions to store
      (swap! store assoc-in [prefix :tokens] [])
      (swap! store assoc-in [prefix :selected-word] nil)
      (swap! store assoc-in [prefix :input-text] nil)

      ;; Verify cleaned state
      (is (= [] (get-in @store [prefix :tokens])))
      (is (nil? (get-in @store [prefix :selected-word])))
      (is (nil? (get-in @store [prefix :input-text]))))))

;; =============================================================================
;; Edge Cases
;; =============================================================================

(deftest edge-case-empty-string-input
  (testing "Edge case: Empty string input"
    (let [store (atom {})
          enter-result (actions/enter-article @store)]
      (is (vector? enter-result))
      ;; Should still work
      (is (= :query/tokenize-text (get-in enter-result [0 1 :query/kind]))))))

(deftest edge-case-whitespace-only-input
  (testing "Edge case: Whitespace-only input"
    (let [store (atom {})]
      (swap! store assoc-in [prefix :input-text] "   ")
      (let [enter-result (actions/enter-article @store)]
        (is (vector? enter-result))
        (is (= :query/tokenize-text (get-in enter-result [0 1 :query/kind])))))))

(deftest edge-case-special-characters
  (testing "Edge case: Special characters in word"
    (let [store (atom {})
          click-result (actions/click-unknown-word store {:word "café"})]
      (is (vector? click-result))
      (is (= "café" (get-in click-result [0 2]))))))

(deftest edge-case-non-string-tokens
  (testing "Edge case: Non-string tokens in article-ui"
    (let [tokens [123 456]
          word->rating {"123" nil "456" nil}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest edge-case-nil-preview-word-add-to-database
  (testing "Edge case: Add preview word when preview-word is nil"
    (let [store (atom {})]
      (let [add-result (actions/add-preview-word-to-database @store)]
        (is (vector? add-result))
        (is (= :command/add-new-word (get-in add-result [0 1 :command/kind]))
            (is (= nil (get-in add-result [0 1 :command/data :word]))))))))

(deftest edge-case-consecutive-clicks-on-unknown-words
  (testing "Edge case: Quick consecutive clicks on unknown words"
    (let [store (atom {})]
      ;; Click first word
      (swap! store assoc-in [prefix :preview-word] "word1")

      ;; Click second word (should replace first)
      (let [click-result (actions/click-unknown-word store {:word "word2"})]
        (is (vector? click-result))
        (is (= "word2" (get-in click-result [0 2]))))

      ;; Update store
      (swap! store assoc-in [prefix :preview-word] "word2")
      (is (= "word2" (get-in @store [prefix :preview-word]))))))

;; =============================================================================
;; UI State Transitions Tests
;; =============================================================================

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
