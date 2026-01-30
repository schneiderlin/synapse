(ns com.zihao.llm-auth.db-test
  (:require [clojure.test :refer :all]
            [com.zihao.llm-auth.db :as db]))

(defn setup [f]
  (db/reset!)
  (f))

(defn teardown [f]
  (f))

(use-fixtures :each setup teardown)

(deftest db-empty?-test
  (testing "Check if database is empty (should have initial users)"
    (let [empty? (db/db-empty?)]
      (is (false? empty?)))))

(deftest get-user-by-username-test
  (testing "Get user by username"
    (let [user (db/get-user-by-username "admin")]
      (is (not (nil? user)))
      (is (= "admin" (:auth-user/username user)))
      (is (= "admin" (:auth-user/password user))))))

(deftest get-user-by-id-test
  (testing "Get user by ID"
    (let [user (db/get-user-by-id "user-001")]
      (is (not (nil? user)))
      (is (= "admin" (:auth-user/username user))))))

(deftest check-credentials-valid-test
  (testing "Check valid credentials"
    (let [result (db/check-credentials "admin" "admin")]
      (is (not (nil? result)))
      (is (= "admin" (:auth-user/username result)))
      (is (= "admin" (:auth-user/role result))))))

(deftest check-credentials-invalid-password-test
  (testing "Check invalid credentials (wrong password)"
    (let [result (db/check-credentials "admin" "wrong")]
      (is (nil? result)))))

(deftest check-credentials-invalid-user-test
  (testing "Check invalid credentials (wrong username)"
    (let [result (db/check-credentials "nonexistent" "admin")]
      (is (nil? result)))))

(deftest add-user-test
  (testing "Add a new user"
    (db/add-user! "testuser" "testpass" "agent" "en")
    (let [user (db/get-user-by-username "testuser")]
      (is (not (nil? user)))
      (is (= "testuser" (:auth-user/username user)))
      (is (= "testpass" (:auth-user/password user)))
      (is (= "agent" (:auth-user/role user))))))

(deftest update-user-language-test
  (testing "Update user preferred language"
    (let [user (:db/id (db/get-user-by-username "admin"))]
      (db/update-user-language! user "zh")
      (let [updated (db/get-user-by-id "user-001")]
        (is (= "zh" (:auth-user/preferred-language updated)))))))

(deftest update-user-role-test
  (testing "Update user role"
    (let [user (:db/id (db/get-user-by-username "agent"))]
      (db/update-user-role! user "admin")
      (let [updated (db/get-user-by-id "user-002")]
        (is (= "admin" (:auth-user/role updated)))))))
