(ns com.zihao.language-learn.lingq.actions-test
  (:require [clojure.test :refer [deftest is testing]]
            [com.zihao.replicant-main.interface :as rm]
            [com.zihao.language-learn.lingq.actions :as actions]
            #_[com.zihao.language-learn.lingq.test-helpers :as helpers]
            [com.zihao.language-learn.lingq.common :refer [prefix]]
            [com.zihao.replicant-main.interface :as rm]))

;; =============================================================================
;; click-unknown-word Action Tests
;; =============================================================================

#_(deftest click-unknown-word-sets-preview-word
  (testing "Clicking unknown word sets preview-word and initiates translation query"
    (let [store (atom {})
          result (actions/click-unknown-word store {:word "anjing"})]
      (is (vector? result))
      (is (= [[:store/assoc-in [prefix :preview-word] "anjing"]
              [:store/assoc-in [prefix :preview-translation] nil]
              [:data/query {:query/kind :query/get-word-translation
                            :query/data {:word "anjing"}}
               {:on-success [[:store/assoc-in [prefix :preview-translation] :query/result]]}]]
             result)))))

(deftest click-unknown-word-sets-preview-word
  (testing "Clicking unknown word sets preview-word and initiates translation query"
    (let [store (atom {})
          result (actions/click-unknown-word store {:word "anjing"})]
      (is (vector? result))
      (is (= [[:store/assoc-in [prefix :preview-word] "anjing"]
              [:store/assoc-in [prefix :preview-translation] nil]
              [:data/query {:query/kind :query/get-word-translation
                            :query/data {:word "anjing"}}
               {:on-success [[:store/assoc-in [prefix :preview-translation] :query/result]]}]]
             result)))))

(deftest click-unknown-word-with-different-word
  (testing "Clicking unknown word with different word parameter"
    (let [store (atom {})
          result (actions/click-unknown-word store {:word "hello"})]
      (is (vector? result))
      (is (= [[:store/assoc-in [prefix :preview-word] "hello"]
              [:store/assoc-in [prefix :preview-translation] nil]
              [:data/query {:query/kind :query/get-word-translation
                            :query/data {:word "hello"}}
               {:on-success [[:store/assoc-in [prefix :preview-translation] :query/result]]}]]
             result)))))

(deftest click-unknown-word-empty-word
  (testing "Clicking unknown word with empty string"
    (let [store (atom {})
          result (actions/click-unknown-word store {:word ""})]
      (is (vector? result))
      (is (= "" (get-in result [0 2]))))))

;; =============================================================================
;; add-preview-word-to-database Action Tests
;; =============================================================================

(deftest add-preview-word-to-database-with-preview-word
  (testing "Adding preview word to database when preview-word is set"
    (let [store (atom {prefix {:preview-word "anjing"
                               :preview-translation ["dog"]}})
          result (actions/add-preview-word-to-database @store)]
      (is (vector? result))
      (is (= [[:data/command {:command/kind :command/add-new-word
                              :command/data {:word "anjing"}}
               {:on-success [[:store/assoc-in [prefix :preview-word] nil]
                             [:store/assoc-in [prefix :preview-translation] nil]
                             [:data/query {:query/kind :query/get-word-rating}]]}]]
             result)))))

(deftest add-preview-word-to-database-nil-preview-word
  (testing "Adding preview word to database when preview-word is nil"
    (let [store (atom {prefix {}})
          result (actions/add-preview-word-to-database @store)]
      (is (vector? result))
      (is (= [[:data/command {:command/kind :command/add-new-word
                             :command/data {:word nil}}
               {:on-success [[:store/assoc-in [prefix :preview-word] nil]
                             [:store/assoc-in [prefix :preview-translation] nil]
                             [:data/query {:query/kind :query/get-word-rating}]]}]]
             result)))))

;; =============================================================================
;; clean-text Action Tests
;; =============================================================================

(deftest clean-text-clears-state
  (testing "Cleaning text clears tokens, selected-word, and input-text"
    (let [store (atom {})
          result (actions/clean-text store)]
      (is (vector? result))
      (is (= [[:store/assoc-in [prefix :tokens] []]
              [:store/assoc-in [prefix :selected-word] nil]
              [:store/assoc-in [prefix :input-text] nil]]
             result)))))

;; =============================================================================
;; enter-article Action Tests
;; =============================================================================

(deftest enter-article-with-input-text
  (testing "Entering article with input-text initiates tokenization"
    (let [store (atom {prefix {:input-text "Halo dunia"}})
          result (actions/enter-article @store)]
      (is (vector? result))
      (is (= [[:data/query {:query/kind :query/tokenize-text
                            :query/data {:language "id"
                                         :text "Halo dunia"}}
               {:on-success [[:data/query {:query/kind :query/get-word-rating}
                              {:on-success [[:store/assoc-in [prefix :word->rating] :query/result]
                                            [:store/assoc-in [prefix :tokens] :query/result]]}]]}]]
             result)))))

(deftest enter-article-empty-input
  (testing "Entering article with empty input-text"
    (let [store (atom {prefix {:input-text ""}})
          result (actions/enter-article @store)]
      (is (vector? result))
      ;; Should still tokenize, just with empty string
      (is (= :query/tokenize-text
             (get-in result [0 1 :query/kind]))))))

(deftest enter-article-nil-input
  (testing "Entering article with nil input-text"
    (let [store (atom {prefix {}})
          result (actions/enter-article @store)]
      (is (vector? result))
      (is (= :query/tokenize-text
             (get-in result [0 1 :query/kind]))))))

;; =============================================================================
;; execute-action Tests
;; =============================================================================

;; TODO: 这个是样例, AI 应该学这个
(deftest execute-action-click-unknown-word
  (testing "Action handler routes to click-unknown-word"
    (let [store (atom {})
          system {:store store}
          event nil
          execute-f (apply rm/make-execute-f [actions/execute-action])]
      (execute-f system event [[:lingq/click-unknown-word {:word "anjing"}]])
      (let [state @store]
        (is (= "anjing" (get-in state [prefix :preview-word])))))))

(deftest execute-action-clean-text
  (testing "Action handler routes to clean-text"
    (let [store (atom {})
          result (actions/execute-action {:store store} nil :lingq/clean-text [])]
      (is (vector? result))
      (is (= 3 (count result))))))

(deftest execute-action-enter-article
  (testing "Action handler routes to enter-article"
    (let [store (atom {})
          result (actions/execute-action {:store store} nil :lingq/enter-article {:article "Halo dunia"})]
      (is (vector? result)))))

(deftest execute-action-add-preview-word-to-database
  (testing "Action handler routes to add-preview-word-to-database"
    (let [store (atom {prefix {:preview-word "test"}})
          result (actions/execute-action {:store store} nil :lingq/add-preview-word-to-database [])]
      (is (vector? result)))))

(deftest execute-action-unknown
  (testing "Action handler returns nil for unknown action"
    (let [store (atom {})
          result (actions/execute-action {:store store} nil :lingq/unknown-action [])]
      (is (nil? result)))))
