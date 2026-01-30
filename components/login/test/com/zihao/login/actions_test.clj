(ns com.zihao.login.actions-test
  (:require [clojure.test :refer :all]
            [com.zihao.login.actions :as actions]))

(deftest login-action-test
  (testing "Returns correct event vector with login command"
    (let [result (actions/login {:username "admin" :password "pass123"})]
      (is (vector? result))
      (is (= [:event/prevent-default] (first result)))
      (is (vector? (second result))))))

(deftest login-action-keys-test
  (testing "Contains expected keys in command data"
    (let [result (actions/login {:username "admin" :password "pass123"})]
      (is (= :data/command (-> result second first)))
      (is (= :command/login (-> result second second :command/kind)))
      (is (= "admin" (-> result second second :command/data :username)))
      (is (= "pass123" (-> result second second :command/data :password))))))

(deftest change-password-action-test
  (testing "Returns correct event vector with change-password command"
    (let [result (actions/change-password {:username "admin" :old-password "old123" :new-password "new123"})]
      (is (vector? result))
      (is (= [:event/prevent-default] (first result)))
      (is (vector? (second result))))))

(deftest change-password-success-handler-test
  (testing "Contains on-success handler"
    (let [result (actions/change-password {:username "admin" :old-password "old123" :new-password "new123"})]
      (is (contains? (nth (second result) 2) :on-success))
      (is (vector? (get-in result [1 2 :on-success]))))))

(deftest change-password-failure-handler-test
  (testing "Contains on-failure handler"
    (let [result (actions/change-password {:username "admin" :old-password "old123" :new-password "new123"})]
      (is (contains? (nth (second result) 2) :on-failure))
      (is (vector? (get-in result [1 2 :on-failure]))))))
