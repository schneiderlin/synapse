(ns com.zihao.language-learn.lingq.render-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.language-learn.lingq.render :as render]
            [com.zihao.language-learn.lingq.word-rating :as word-rating]
            [com.zihao.language-learn.lingq.common :refer [prefix]]
            [com.zihao.replicant-main.hiccup-test-helper :refer
             [contains-element?
              contains-element-with-attr?
              contains-element-with-content?
              contains-hiccup?]]))

;; =============================================================================
;; textarea-ui Tests
;; =============================================================================

(deftest textarea-ui-contains-textarea
  (testing "textarea-ui contains a textarea element"
    (let [result (render/textarea-ui)]
      (is (contains-element? result :textarea)))))

(deftest textarea-ui-contains-process-button
  (testing "textarea-ui contains a Process Article button"
    (let [result (render/textarea-ui)]
      (is (contains-element-with-content? result "Process Article")))))

(deftest textarea-ui-contains-clear-button
  (testing "textarea-ui contains a Clear Text button"
    (let [result (render/textarea-ui)]
      (is (contains-element-with-content? result "Clear Text")))))

(deftest textarea-ui-textarea-has-on-attribute
  (testing "textarea-ui textarea has :on attribute"
    (let [result (render/textarea-ui)]
      (is (contains-hiccup? result
                            #(and (vector? %)
                                  (= :textarea (first %))
                                  (map? (second %))
                                  (contains? (second %) :on)))))))

(deftest textarea-ui-textarea-has-input-key
  (testing "textarea-ui textarea has :input key in :on attribute"
    (let [result (render/textarea-ui)]
      (is (contains-hiccup? result
                            #(and (vector? %)
                                  (= :textarea (first %))
                                  (map? (second %))
                                  (map? (get (second %) :on))
                                  (contains? (get (second %) :on) :input)))))))

(deftest textarea-ui-process-button-has-click-action
  (testing "Process Article button has :on attribute with :click key"
    (let [result (render/textarea-ui)]
      (is (contains-hiccup? result
                            #(and (vector? %)
                                  (= :button (first %))
                                  (contains-element-with-content? % "Process Article")
                                  (map? (second %))
                                  (contains? (second %) :on)))))))

(deftest textarea-ui-clear-button-has-click-action
  (testing "Clear Text button has :on attribute with :click key"
    (let [result (render/textarea-ui)]
      (is (contains-hiccup? result
                            #(and (vector? %)
                                  (= :button (first %))
                                  (contains-element-with-content? % "Clear Text")
                                  (map? (second %))
                                  (contains? (second %) :on)))))))

;; =============================================================================
;; main Tests - UI State Transitions
;; =============================================================================

(deftest ui-state-transition-textarea-to-article
  (testing "UI state transition: textarea to article"
    (let [store (atom {})]

      ;; Initial state: no tokens (textarea shown)
      (let [state @store
            result (render/main state)]
        ;; Should contain textarea element
        (is (contains-element? result :textarea))
        ;; Should contain "Process Article" button
        (is (contains-element-with-content? result "Process Article"))
        ;; Should NOT contain "Clear Article" button
        (is (not (contains-element-with-content? result "Clear Article"))))

      ;; After enter-article: tokens present (article shown)
      (swap! store assoc-in [prefix :tokens] ["Halo" " " "dunia"])
      (let [state @store
            result (render/main state)]
        ;; Should contain "Clear Article" button
        (is (contains-element-with-content? result "Clear Article"))
        ;; Should NOT contain textarea element (article view is shown instead)
        (is (not (contains-element? result :textarea)))))))

(deftest ui-state-transition-show-preview
  (testing "UI state transition: Show preview panel"
    (let [store (atom {})]

      ;; Initial state: no preview (preview panel not shown)
      (let [state @store
            result (render/main state)]
        ;; Should NOT contain preview panel elements
        (is (not (contains-element-with-content? result "Word Preview")))
        (is (not (contains-element-with-content? result "原文: ")))
        (is (not (contains-element-with-content? result "译文: "))))

      ;; Click unknown word: preview panel shown
      (swap! store assoc-in [prefix :preview-word] "anjing")
      (swap! store assoc-in [prefix :preview-translation] ["calm" "peaceful"])
      (let [state @store
            result (render/main state)]
        ;; Should contain preview panel header
        (is (contains-element-with-content? result "Word Preview"))
        ;; Should contain the preview word
        (is (contains-element-with-content? result "anjing"))
        ;; Should contain translations (note: "原文: " and "译文: " have trailing spaces)
        (is (contains-element-with-content? result "原文: "))
        (is (contains-element-with-content? result "译文: "))
        (is (contains-element-with-content? result "calm, peaceful"))
        ;; Should contain "添加到数据库" button
        (is (contains-element-with-content? result "添加到数据库"))))))

(deftest ui-state-transition-show-rating
  (testing "UI state transition: Show rating panel"
    (let [store (atom {})]

      ;; Initial state: no selection (rating panel not shown)
      (let [state @store
            result (render/main state)]
        ;; Should NOT contain rating buttons
        (is (not (contains-element-with-content? result "again")))
        (is (not (contains-element-with-content? result "hard")))
        (is (not (contains-element-with-content? result "good")))
        (is (not (contains-element-with-content? result "easy"))))

      ;; Click known word: rating panel shown
      (swap! store assoc-in [prefix :tokens] ["anjing"])
      (swap! store assoc-in [prefix :selected-word] "anjing")
      (let [state @store
            result (render/main state)]
        ;; Should contain all rating buttons
        (is (contains-element-with-content? result "again"))
        (is (contains-element-with-content? result "hard"))
        (is (contains-element-with-content? result "good"))
        (is (contains-element-with-content? result "easy"))
        ;; Should contain button elements
        (is (contains-element? result :button))))))

(deftest main-empty-state-shows-textarea
  (testing "main with empty state shows textarea-ui"
    (let [result (render/main {})]
      (is (contains-element? result :textarea))
      (is (contains-element-with-content? result "Process Article"))
      (is (contains-element-with-content? result "Clear Text"))
      (is (not (contains-element-with-content? result "Clear Article"))))))

(deftest main-with-empty-tokens-shows-textarea
  (testing "main without tokens shows textarea-ui"
    (let [state {prefix {:tokens []}}
          result (render/main state)]
      (is (contains-element? result :textarea))
      (is (contains-element-with-content? result "Process Article"))
      (is (not (contains-element-with-content? result "Clear Article"))))))

(deftest main-with-tokens-shows-article
  (testing "main with tokens shows article-ui"
    (let [state {prefix {:tokens ["test"]}}
          result (render/main state)]
      (is (not (contains-element? result :textarea)))
      (is (not (contains-element-with-content? result "Process Article")))
      (is (contains-element-with-content? result "Clear Article"))
      (is (contains-element-with-content? result "Article Text")))))

(deftest main-with-preview-word-shows-preview-panel
  (testing "main with preview-word shows preview panel"
    (let [state {prefix {:preview-word "test" :preview-translation ["translation"]}}
          result (render/main state)]
      (is (contains-element-with-content? result "Word Preview"))
      (is (contains-element-with-content? result "原文: "))
      (is (contains-element-with-content? result "译文: "))
      (is (contains-element-with-content? result "test"))
      (is (contains-element-with-content? result "translation")))))

(deftest main-with-selected-word-shows-rating-panel
  (testing "main with selected-word shows rating panel when no preview-word"
    (let [state {prefix {:selected-word "test" :tokens ["test"]}}
          result (render/main state)]
      (is (contains-element-with-content? result "again"))
      (is (contains-element-with-content? result "hard"))
      (is (contains-element-with-content? result "good"))
      (is (contains-element-with-content? result "easy")))))

(deftest main-with-both-selected-and-preview-shows-preview
  (testing "main with both selected-word and preview-word shows preview panel"
    (let [state {prefix {:selected-word "test1"
                          :preview-word "test2"
                          :preview-translation ["trans"]}}
          result (render/main state)]
      ;; Should show preview panel, not rating panel
      (is (contains-element-with-content? result "Word Preview"))
      (is (contains-element-with-content? result "test2")))))

;; =============================================================================
;; word-rating-ui Tests (from word-rating namespace)
;; =============================================================================

(deftest word-rating-ui-contains-rating-buttons
  (testing "word-rating-ui contains rating buttons"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (contains-element? result :button)))))

(deftest word-rating-ui-contains-all-rating-texts
  (testing "word-rating-ui contains all rating texts"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (contains-element-with-content? result "again"))
      (is (contains-element-with-content? result "hard"))
      (is (contains-element-with-content? result "good"))
      (is (contains-element-with-content? result "easy")))))

(deftest word-rating-ui-buttons-have-on-attribute
  (testing "word-rating-ui buttons have :on attribute"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (contains-hiccup? result
                            #(and (vector? %)
                                  (= :button (first %))
                                  (map? (second %))
                                  (contains? (second %) :on)))))))

;; =============================================================================
;; button Tests (word-rating helper)
;; =============================================================================

(deftest button-has-required-attributes
  (testing "button has on attribute"
    (let [result (word-rating/button {:on {:click [:test]} :variant "default" :text "Test"})]
      (is (contains-element? result :button))
      (is (contains-element-with-attr? result :on)))))
