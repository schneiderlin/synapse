(ns com.zihao.playground-stasis.core-test
  (:require [clojure.test :refer :all]
            [com.zihao.playground-stasis.core :as core]))

(deftest pages-test
  (testing "Returns map with /index.html and /test.html"
    (let [result (core/pages)]
      (is (map? result))
      (is (contains? result "/index.html"))
      (is (contains? result "/test.html"))
      (is (string? (get result "/index.html")))
      (is (string? (get result "/test.html"))))))

(deftest app-index-test
  (testing "Handler returns correct response for /index.html"
    (let [request {:request-method :get :uri "/index.html"}
          response (core/app request)]
      (is (map? response))
      (is (contains? response :body))
      (is (string? (:body response)))
      (is (.contains (:body response) "Welcome!")))))

(deftest app-test-test
  (testing "Handler returns correct response for /test.html"
    (let [request {:request-method :get :uri "/test.html"}
          response (core/app request)]
      (is (map? response))
      (is (contains? response :body))
      (is (string? (:body response)))
      (is (.contains (:body response) "Test!")))))

(deftest app-notfound-test
  (testing "Handler returns 404 for unknown routes"
    (let [request {:request-method :get :uri "/unknown.html"}
          response (core/app request)]
      (is (map? response))
      (is (= 404 (:status response)))
      (is (.contains (:body response) "Page not found")))))
