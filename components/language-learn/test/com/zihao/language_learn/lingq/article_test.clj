(ns com.zihao.language-learn.lingq.article-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.language-learn.lingq.article :as article]))

;; =============================================================================
;; get-word-class Tests
;; =============================================================================

(deftest get-word-class-known
  (testing "Word class for known word"
    (let [result (article/get-word-class "known")]
      (is (vector? result))
      (is (contains? (set result) "px-0.5"))
      (is (contains? (set result) "py-0.5"))
      (is (contains? (set result) "rounded"))
      (is (contains? (set result) "text-lg")))))

(deftest get-word-class-rating-1
  (testing "Word class for rating 1"
    (let [result (article/get-word-class 1)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-orange-900"))
      (is (contains? (set result) "text-white")))))

(deftest get-word-class-rating-2
  (testing "Word class for rating 2"
    (let [result (article/get-word-class 2)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-orange-800")))))

(deftest get-word-class-rating-3
  (testing "Word class for rating 3"
    (let [result (article/get-word-class 3)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-orange-600")))))

(deftest get-word-class-rating-4
  (testing "Word class for rating 4"
    (let [result (article/get-word-class 4)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-orange-500")))))

(deftest get-word-class-rating-5
  (testing "Word class for rating 5"
    (let [result (article/get-word-class 5)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-orange-400")))))

(deftest get-word-class-nil
  (testing "Word class for nil rating (unknown word)"
    (let [result (article/get-word-class nil)]
      (is (vector? result))
      (is (contains? (set result) "cursor-pointer"))
      (is (contains? (set result) "bg-blue-600")))))

(deftest get-word-class-unknown
  (testing "Word class for unknown rating"
    (let [result (article/get-word-class :unknown)]
      (is (vector? result))
      (is (contains? (set result) "unknown-rating")))))

;; =============================================================================
;; article-ui Tests
;; =============================================================================

(deftest article-ui-empty-tokens
  (testing "Article UI with empty tokens"
    (let [result (article/article-ui {:tokens [] :word->rating {}})]
      (is (vector? result))
      (is (= :div (first result)))
      (is (map? (second result))))))

(deftest article-ui-simple-tokens
  (testing "Article UI with simple word tokens"
    (let [tokens ["Halo" " " "dunia"]
          word->rating {"halo" 1 "dunia" nil}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (is (= :div (first result)))
      ;; Check that tokens are rendered
      (is (some #(and (vector? %) (= :span (first %))) (rest result))))))

(deftest article-ui-whitespace-not-wrapped
  (testing "Whitespace tokens are not wrapped in span"
    (let [tokens ["Hello" " " "world"]
          word->rating {"hello" "known" "world" "known"}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      ;; The space should be a plain string, not a span
      (let [rendered (rest result)]
        (is (some #(= " " %) rendered))))))

(deftest article-ui-punctuation-not-wrapped
  (testing "Punctuation tokens are not wrapped in span"
    (let [tokens ["Hello" "," " " "world" "!"]
          word->rating {"hello" "known" "world" "known"}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (let [rendered (rest result)]
        (is (some #(= "," %) rendered))
        (is (some #(= "!" %) rendered))))))

(deftest article-ui-numbers-not-wrapped
  (testing "Number tokens are not wrapped in span"
    (let [tokens ["I" " " "have" " " "2" " " "apples"]
          word->rating {"i" "known" "have" "known" "apples" "known"}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (let [rendered (rest result)]
        (is (some #(= "2" %) rendered))))))

(deftest article-ui-unknown-word-clickable
  (testing "Unknown word has click action"
    (let [tokens ["unknown"]
          word->rating {"unknown" nil}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (let [span (first (filter #(and (vector? %) (= :span (first %))) (rest result)))]
        (is (some? span))
        (is (contains? (second span) :on))))))

(deftest article-ui-known-word-clickable
  (testing "Known word has click action for selected-word"
    (let [tokens ["known"]
          word->rating {"known" "known"}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      (let [span (first (filter #(and (vector? %) (= :span (first %))) (rest result)))]
        (is (some? span))
        (is (contains? (second span) :on))))))

(deftest article-ui-non-string-token
  (testing "Handles non-string tokens gracefully"
    (let [tokens [123 456]
          word->rating {}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      ;; Should convert to string using str
      (let [spans (filter #(and (vector? %) (= :span (first %))) (rest result))]
        (is (= 2 (count spans)))))))

(deftest article-ui-mixed-tokens
  (testing "Handles mixed string and non-string tokens"
    (let [tokens ["Hello" 123 "world"]
          word->rating {"hello" "known" "123" nil "world" nil}
          result (article/article-ui {:tokens tokens :word->rating word->rating})]
      (is (vector? result))
      ;; Should render tokens appropriately
      (let [spans (filter #(and (vector? %) (= :span (first %))) (rest result))]
        (is (pos? (count spans)))))))

(deftest article-ui-styles
  (testing "Article UI has correct styles"
    (let [result (article/article-ui {:tokens ["test"] :word->rating {}})]
      (is (vector? result))
      (let [attrs (second result)]
        (is (contains? attrs :style))
        (is (contains? (:style attrs) :white-space))
        (is (contains? (:style attrs) :font-size))
        (is (contains? (:style attrs) :line-height))))))