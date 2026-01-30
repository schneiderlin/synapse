(ns com.zihao.kraken-ui.core-test
  (:require [clojure.test :refer :all]
            [com.zihao.kraken-ui.core :as core]))

(deftest piece->text-corner-test
  (testing "Returns \"⚫\" for :corner piece"
    (is (= "⚫" (core/piece->text :corner)))))

(deftest piece->text-kraken-test
  (testing "Returns \"🐙\" for :kraken piece"
    (is (= "🐙" (core/piece->text :kraken)))))

(deftest piece->text-ship-test
  (testing "Returns \"⛵\" for :ship piece"
    (is (= "⛵" (core/piece->text :ship)))))

(deftest piece->text-flagship-test
  (testing "Returns \"🚢\" for :flagship piece"
    (is (= "🚢" (core/piece->text :flagship)))))

(deftest piece->text-nil-test
  (testing "Returns \"\" for :nil piece"
    (is (= " " (core/piece->text :nil)))))

(deftest piece->text-unknown-test
  (testing "Returns \"\" for unknown piece types"
    (is (= "" (core/piece->text :unknown)))))
