(ns com.zihao.llm-auth.actions-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.llm-auth.actions :as actions]))

(deftest login-action-test
  (testing "Returns correct event vector with login command"
    (let [result (actions/login {:username "admin" :password "admin"})]
      (is (vector? result))
      (is (= [:event/prevent-default] (first result)))
      (is (vector? (second result))))))

(deftest login-action-keys-test
  (testing "Contains expected keys in command data"
    (let [result (actions/login {:username "admin" :password "admin"})]
      (is (= :data/command (-> result second first)))
      (is (= :llm-auth/login (-> result second second :command/kind)))
      (is (= "admin" (-> result second second :command/data :username)))
      (is (= "admin" (-> result second second :command/data :password))))))

(deftest logout-action-test
  (testing "Returns correct event vector with logout command"
    (let [result (actions/logout)]
      (is (vector? result))
      (is (vector? (first result))))))

(deftest logout-action-keys-test
  (testing "Contains expected keys in command data"
    (let [result (actions/logout)]
      (is (= :data/command (-> result first first)))
      (is (= :llm-auth/logout (-> result first second :command/kind))))))

(deftest update-language-action-test
  (testing "Returns correct event vector with update-language command"
    (let [result (actions/update-language {:user-id "user-001" :language "zh"})]
      (is (vector? result))
      (is (vector? (first result))))))

(deftest update-language-action-keys-test
  (testing "Contains expected keys in command data"
    (let [result (actions/update-language {:user-id "user-001" :language "zh"})]
      (is (= :data/command (-> result first first)))
      (is (= :llm-auth/update-language (-> result first second :command/kind)))
      (is (= "user-001" (-> result first second :command/data :user-id)))
      (is (= "zh" (-> result first second :command/data :language))))))

(deftest update-role-action-test
  (testing "Returns correct event vector with update-role command"
    (let [result (actions/update-role {:user-id "user-002" :role :admin})]
      (is (vector? result))
      (is (vector? (first result))))))

(deftest update-role-action-keys-test
  (testing "Contains expected keys in command data"
    (let [result (actions/update-role {:user-id "user-002" :role :admin})]
      (is (= :data/command (-> result first first)))
      (is (= :llm-auth/update-role (-> result first second :command/kind)))
      (is (= "user-002" (-> result first second :command/data :user-id)))
      (is (= :admin (-> result first second :command/data :role))))))

(deftest execute-action-test
  (testing "Execute action returns correct vectors"
    (let [store (atom {})
          system {:store store}]
      (is (vector? (actions/execute-action system nil :llm-auth/login {:username "admin" :password "admin"})))
      (is (vector? (actions/execute-action system nil :llm-auth/logout {})))
      (is (vector? (actions/execute-action system nil :llm-auth/update-language {:user-id "user-001" :language "zh"})))
      (is (vector? (actions/execute-action system nil :llm-auth/update-role {:user-id "user-002" :role :admin})))
      (is (nil? (actions/execute-action system nil :unknown/action {}))))))
