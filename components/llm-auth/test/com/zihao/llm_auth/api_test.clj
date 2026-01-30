(ns com.zihao.llm-auth.api-test
  (:require [clojure.test :refer :all]
            [com.zihao.llm-auth.api :as api]
            [com.zihao.llm-auth.db :as db]))

(deftest command-handler-login-success-test
  (testing "Login with valid credentials returns user data"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/login
                                       :command/data {:username "admin"
                                                      :password "admin"}})]
      (is (not (nil? result)))
      (is (true? (:success? result)))
      (is (contains? result :user))
      (is (= "admin" (get-in result [:user :username])))
      (is (= :admin (get-in result [:user :role])))
      (is (contains? result :permissions)))))

(deftest command-handler-login-failure-test
  (testing "Login with invalid credentials returns nil"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/login
                                       :command/data {:username "admin"
                                                      :password "wrong"}})]
      (is (nil? result)))))

(deftest command-handler-logout-test
  (testing "Logout command returns success"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/logout
                                       :command/data {}})]
      (is (not (nil? result)))
      (is (true? (:success? result))))))

(deftest command-handler-update-language-test
  (testing "Update user language returns success"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/update-language
                                       :command/data {:user-id "user-001"
                                                      :language "zh"}})]
      (is (not (nil? result)))
      (is (true? (:success? result))))))

(deftest command-handler-update-role-valid-test
  (testing "Update user role with valid role returns success"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/update-role
                                       :command/data {:user-id "user-002"
                                                      :role :admin}})]
      (is (not (nil? result)))
      (is (true? (:success? result))))))

(deftest command-handler-update-role-invalid-test
  (testing "Update user role with invalid role returns nil"
    (let [result (api/command-handler nil
                                      {:command/kind :llm-auth/update-role
                                       :command/data {:user-id "user-002"
                                                      :role :invalid}})]
      (is (nil? result)))))

(deftest command-handler-unknown-test
  (testing "Unknown command returns nil"
    (let [result (api/command-handler nil
                                      {:command/kind :unknown/command
                                       :command/data {}})]
      (is (nil? result)))))

(deftest query-handler-navigation-admin-test
  (testing "Get navigation items for admin role"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/navigation
                                     :query/data {:role :admin}})]
      (is (not (nil? result)))
      (is (contains? result :navigation))
      (is (> (count (:navigation result)) 0)))))

(deftest query-handler-navigation-agent-test
  (testing "Get navigation items for agent role"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/navigation
                                     :query/data {:role :agent}})]
      (is (not (nil? result)))
      (is (contains? result :navigation))
      (is (> (count (:navigation result)) 0)))))

(deftest query-handler-permissions-test
  (testing "Get permissions for a role"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/permissions
                                     :query/data {:role :admin}})]
      (is (not (nil? result)))
      (is (contains? result :permissions))
      (is (> (count (:permissions result)) 0)))))

(deftest query-handler-check-permission-test
  (testing "Check if role has permission"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/check-permission
                                     :query/data {:role :admin
                                                  :permission :users/manage}})]
      (is (not (nil? result)))
      (is (true? (:has-permission? result))))))

(deftest query-handler-check-permission-false-test
  (testing "Check if role lacks permission"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/check-permission
                                     :query/data {:role :agent
                                                  :permission :users/manage}})]
      (is (not (nil? result)))
      (is (false? (:has-permission? result))))))

(deftest query-handler-roles-test
  (testing "Get all available roles"
    (let [result (api/query-handler nil
                                    {:query/kind :llm-auth/roles
                                     :query/data {}})]
      (is (not (nil? result)))
      (is (contains? result :roles))
      (is (contains? (:roles result) :admin))
      (is (contains? (:roles result) :agent)))))

(deftest query-handler-unknown-test
  (testing "Unknown query returns nil"
    (let [result (api/query-handler nil
                                    {:query/kind :unknown/query
                                     :query/data {}})]
      (is (nil? result)))))
