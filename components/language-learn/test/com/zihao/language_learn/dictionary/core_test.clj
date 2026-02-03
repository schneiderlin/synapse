(ns com.zihao.language-learn.dictionary.core-test
  (:require [clojure.test :refer [deftest is testing]]
            [clojure.string :as str]
            [com.zihao.language-learn.dictionary.core :as dict]
            [clj-http.client :as http]))

(defn mock-response
  "Creates a mock HTTP response with the given HTML body"
  [body]
  {:status 200
   :body body
   :headers {}})

(def sample-html-response
  "<html><body>
    <section>
      <div><div>
        <ul>
          <li data-element='translation'>
            <div><h3>dog</h3></div>
          </li>
          <li data-element='translation'>
            <div><h3>canine</h3></div>
          </li>
          <li data-element='translation'>
            <div><h3>hound</h3></div>
          </li>
        </ul>
      </div></div>
    </section>
  </body></html>")

(def empty-html-response
  "<html><body>
    <section>
      <div><div>
        <ul>
        </ul>
      </div></div>
    </section>
  </body></html>")

(def single-translation-response
  "<html><body>
    <section>
      <div><div>
        <ul>
          <li data-element='translation'>
            <div><h3>hello</h3></div>
          </li>
        </ul>
      </div></div>
    </section>
  </body></html>")

(deftest get-translations-multiple-words-test
  (testing "Successfully retrieves multiple translation words"
    (with-redefs [http/get (constantly (mock-response sample-html-response))]
      (let [result (dict/get-translations "anjing" "id" "en")]
        (is (vector? result))
        (is (= 3 (count result)))
        (is (contains? (set result) "dog"))
        (is (contains? (set result) "canine"))
        (is (contains? (set result) "hound"))))))

(deftest get-translations-single-word-test
  (testing "Successfully retrieves a single translation word"
    (with-redefs [http/get (constantly (mock-response single-translation-response))]
      (let [result (dict/get-translations "hello" "en" "es")]
        (is (vector? result))
        (is (= 1 (count result)))
        (is (= ["hello"] result))))))

(deftest get-translations-empty-test
  (testing "Handles empty translation list"
    (with-redefs [http/get (constantly (mock-response empty-html-response))]
      (let [result (dict/get-translations "unknown" "xx" "yy")]
        (is (vector? result))
        (is (= 0 (count result)))))))

(deftest get-translations-trims-whitespace-test
  (testing "Trims whitespace from translation words"
    (let [html-with-whitespace "<html><body>
          <section>
            <div><div>
              <ul>
                <li data-element='translation'>
                  <div><h3>  hello world  </h3></div>
                </li>
                <li data-element='translation'>
                  <div><h3>  foo bar  </h3></div>
                </li>
              </ul>
            </div></div>
          </section>
        </body></html>"]
      (with-redefs [http/get (constantly (mock-response html-with-whitespace))]
        (let [result (dict/get-translations "test" "en" "es")]
          (is (vector? result))
          (is (= ["hello world" "foo bar"] result))
          (is (every? #(= % (str/trim %)) result)))))))

(deftest get-translations-different-language-pairs-test
  (testing "Works with different language pairs"
    (with-redefs [http/get (constantly (mock-response single-translation-response))]
      ;; Indonesian to English
      (let [result1 (dict/get-translations "anjing" "id" "en")]
        (is (vector? result1)))
      ;; English to Spanish
      (let [result2 (dict/get-translations "hello" "en" "es")]
        (is (vector? result2)))
      ;; French to English
      (let [result3 (dict/get-translations "bonjour" "fr" "en")]
        (is (vector? result3))))))

(deftest get-translations-special-characters-test
  (testing "Handles words with special characters"
    (let [html-with-special "<html><body>
          <section>
            <div><div>
              <ul>
                <li data-element='translation'>
                  <div><h3>café</h3></div>
                </li>
                <li data-element='translation'>
                  <div><h3>résumé</h3></div>
                </li>
              </ul>
            </div></div>
          </section>
        </body></html>"]
      (with-redefs [http/get (constantly (mock-response html-with-special))]
        (let [result (dict/get-translations "café" "fr" "en")]
          (is (vector? result))
          (is (= 2 (count result)))
          (is (contains? (set result) "café"))
          (is (contains? (set result) "résumé")))))))

(deftest get-translations-order-preserved-test
  (testing "Preserves the order of translations from the response"
    (with-redefs [http/get (constantly (mock-response sample-html-response))]
      (let [result (dict/get-translations "test" "en" "es")]
        (is (vector? result))
        (is (= ["dog" "canine" "hound"] result))))))

;; Integration tests - skip by default
(deftest ^:integration get-translations-integration-test
  (testing "Integration test with real API call"
    (let [result (dict/get-translations "hello" "en" "es")]
      (is (vector? result))
      (is (pos? (count result)))
      (is (every? string? result)))))

(deftest ^:integration get-translations-integration-indonesian-english-test
  (testing "Integration test: Indonesian to English"
    (let [result (dict/get-translations "anjing" "id" "en")]
      (is (vector? result))
      (is (pos? (count result)))
      (is (some #(str/includes? (str/lower-case %) "dog") result)))))

(deftest ^:integration get-translations-integration-french-english-test
  (testing "Integration test: French to English"
    (let [result (dict/get-translations "bonjour" "fr" "en")]
      (is (vector? result))
      (is (pos? (count result))))))
