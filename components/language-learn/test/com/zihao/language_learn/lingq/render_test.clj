(ns com.zihao.language-learn.lingq.render-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.language-learn.lingq.render :as render]
            [com.zihao.language-learn.lingq.word-rating :as word-rating]
            [com.zihao.language-learn.lingq.common :refer [prefix]]))

;; =============================================================================
;; textarea-ui Tests
;; =============================================================================

(deftest textarea-ui-structure
  (testing "textarea-ui returns correct structure"
    (let [result (render/textarea-ui)]
      (is (vector? result))
      (is (= :div (first result)))
      (is (map? (second result))))))

(deftest textarea-ui-contains-textarea
  (testing "textarea-ui contains a textarea element"
    (let [result (render/textarea-ui)
          content (drop 2 result)]  ;; Skip :div and attributes
      (is (some #(and (vector? %) (= :textarea (first %))) content)))))

(deftest textarea-ui-contains-process-button
  (testing "textarea-ui contains a Process Article button"
    (let [result (render/textarea-ui)
          content (drop 2 result)]  ;; Skip :div and attributes
      (is (some #(and (vector? %) (= :button (first %)) (some (fn [x] (= "Process Article" x)) (flatten %))) content)))))

(deftest textarea-ui-contains-clear-button
  (testing "textarea-ui contains a Clear Text button"
    (let [result (render/textarea-ui)
          content (drop 2 result)]  ;; Skip :div and attributes
      (is (some #(and (vector? %) (= :button (first %)) (some (fn [x] (= "Clear Text" x)) (flatten %))) content)))))

(deftest textarea-ui-textarea-attributes
  (testing "textarea-ui has correct textarea attributes"
    (let [result (render/textarea-ui)
          textarea (first (filter #(and (vector? %) (= :textarea (first %))) (drop 2 result)))]
      (is (some? textarea))
      (let [attrs (second textarea)]
        (is (contains? attrs :class))
        (is (contains? attrs :placeholder))
        (is (contains? attrs :on))))))

(deftest textarea-ui-input-action
  (testing "textarea-ui textarea has input action"
    (let [result (render/textarea-ui)
          textarea (first (filter #(and (vector? %) (= :textarea (first %))) (drop 2 result)))]
      (is (some? textarea))
      (let [attrs (second textarea)]
        (is (contains? attrs :on))
        ;; The :on attr is a map with :input key
        (is (= [[:store/assoc-in [prefix :input-text] :event/target.value]] (get-in attrs [:on :input])))))))

(deftest textarea-ui-process-action
  (testing "Process Article button has correct action"
    (let [result (render/textarea-ui)
          buttons (filter #(and (vector? %) (= :button (first %))) (drop 2 result))]
      (is (some #(= "Process Article" (last (filter string? (flatten %)))) buttons)))))

(deftest textarea-ui-clear-action
  (testing "Clear Text button has correct action"
    (let [result (render/textarea-ui)
          buttons (filter #(and (vector? %) (= :button (first %))) (drop 2 result))]
      (is (some #(= "Clear Text" (last (filter string? (flatten %)))) buttons)))))

;; =============================================================================
;; main Tests
;; =============================================================================

(deftest main-empty-state
  (testing "main with empty state shows textarea-ui"
    (let [result (render/main {})]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest main-without-tokens
  (testing "main without tokens shows textarea-ui"
    (let [state {prefix {:tokens []}}
          result (render/main state)]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest main-with-tokens
  (testing "main with tokens shows article-ui"
    (let [state {prefix {:tokens ["test"]}}
          result (render/main state)]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest main-with-preview-word
  (testing "main with preview-word shows preview panel"
    (let [state {prefix {:preview-word "test" :preview-translation ["translation"]}}
          result (render/main state)]
      (is (vector? result))
      (is (some #(and (vector? %) (= :div (first %))) (rest result))))))

(deftest main-with-selected-word
  (testing "main with selected-word shows rating panel when no preview-word"
    (let [state {prefix {:selected-word "test" :tokens ["test"]}}
          result (render/main state)]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest main-with-both-selected-and-preview
  (testing "main with both selected-word and preview-word shows preview panel"
    (let [state {prefix {:selected-word "test1"
                          :preview-word "test2"
                          :preview-translation ["trans"]}}
          result (render/main state)]
      (is (vector? result))
      ;; Should show preview panel
      (let [content (rest result)]
        (is (some #(and (vector? %) (= :div (first %))) content))))))

(deftest main-structure
  (testing "main has correct outer structure"
    (let [result (render/main {})]
      (is (vector? result))
      (is (= :div (first result)))
      (let [attrs (second result)]
        (is (contains? attrs :class))))))

;; =============================================================================
;; word-rating-ui Tests (from word-rating namespace)
;; =============================================================================

(deftest word-rating-ui-structure
  (testing "word-rating-ui returns correct structure"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (vector? result))
      (is (= :div (first result))))))

(deftest word-rating-ui-contains-buttons
  (testing "word-rating-ui contains rating buttons"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (vector? result))
      ;; Content is wrapped in parentheses, so we need to extract it properly
      (let [content (drop 2 result)]  ;; Skip :div and attributes
        (is (some #(and (vector? %) (= :button (first %))) content))))))

(deftest word-rating-ui-all-ratings
  (testing "word-rating-ui contains all rating buttons"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (vector? result))
      (let [buttons (filter #(and (vector? %) (= :button (first %))) (drop 2 result))]
        (is (= 4 (count buttons)))
        ;; Check that all ratings are represented
        (let [texts (map #(last (filter string? (flatten %))) buttons)]
          (is (contains? (set texts) "again"))
          (is (contains? (set texts) "hard"))
          (is (contains? (set texts) "good"))
          (is (contains? (set texts) "easy")))))))

(deftest word-rating-ui-selected-rating
  (testing "word-rating-ui highlights selected rating"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating :good})]
      (is (vector? result))
      (let [content (drop 2 result)]
        (is (some #(and (vector? %) (= :button (first %))) content))))))

(deftest word-rating-ui-button-action
  (testing "word-rating-ui buttons have correct action"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (vector? result))
      (let [buttons (filter #(and (vector? %) (= :button (first %))) (drop 2 result))]
        (is (every? #(contains? (second %) :on) buttons))))))

(deftest word-rating-ui-contains-command
  (testing "word-rating-ui buttons contain command action"
    (let [result (word-rating/word-rating-ui {:word "test" :current-rating nil})]
      (is (vector? result))
      (let [buttons (filter #(and (vector? %) (= :button (first %))) (drop 2 result))]
        (is (some #(and (map? (second %))
                        (contains? (:on (second %)) :data/command))
                  buttons))))))

;; =============================================================================
;; button Tests (word-rating helper)
;; =============================================================================

(deftest button-default-variant
  (testing "button with default variant has correct classes"
    (let [result (word-rating/button {:on {:click [:test]} :variant "default" :text "Test"})]
      (is (vector? result))
      (is (= :button (first result)))
      (let [attrs (second result)]
        (is (contains? attrs :class))
        (is (contains? attrs :on))))))

(deftest button-outline-variant
  (testing "button with outline variant has correct classes"
    (let [result (word-rating/button {:on {:click [:test]} :variant "outline" :text "Test"})]
      (is (vector? result))
      (is (= :button (first result))))))

(deftest button-required-props
  (testing "button requires on, variant, and text props"
    (let [result (word-rating/button {:on {:click [:test]} :variant "default" :text "Test"})]
      (is (vector? result))
      (let [attrs (second result)]
        (is (contains? attrs :on))))))
