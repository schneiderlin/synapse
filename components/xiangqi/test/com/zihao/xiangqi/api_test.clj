(ns com.zihao.xiangqi.api-test
  (:require [clojure.test :refer :all]
            [com.zihao.xiangqi.api :as api]
            [com.zihao.xiangqi.core :as core]))

(deftest query-handler-import-game-tree-test
  (testing "Handle :query/import-game-tree query (may throw if file doesn't exist)"
    (is (thrown? java.io.FileNotFoundException
                 (api/query-handler nil {:query/kind :query/import-game-tree})))))

(deftest query-handler-unknown-test
  (testing "Return nil for unknown query kinds"
    (let [result (api/query-handler nil {:query/kind :unknown/query})]
      (is (nil? result)))))

(deftest command-handler-export-game-tree-test
  (testing "Handle :command/export-game-tree command (may throw if directory doesn't exist)"
    (let [game-tree {:test "data"}]
      (is (thrown? java.io.FileNotFoundException
                   (api/command-handler nil {:command/kind :command/export-game-tree :data {:game-tree game-tree}}))))))

(deftest command-handler-export-game-tree-test
  (testing "Handle :command/export-game-tree command (may throw if directory doesn't exist)"
    (let [game-tree {:test "data"}
          exception-caught (atom false)]
      (try
        (api/command-handler nil {:command/kind :command/export-game-tree :data {:game-tree game-tree}})
        (catch java.io.FileNotFoundException e
          (reset! exception-caught true)))
      (is @exception-caught))))

(deftest command-handler-unknown-test
  (testing "Return nil for unknown command kinds"
    (let [result (api/command-handler nil {:command/kind :unknown/command :data {}})]
      (is (nil? result)))))

(deftest command-handler-unknown-test
  (testing "Return nil for unknown command kinds"
    (let [result (api/command-handler nil {:command/kind :unknown/command :data {}})]
      (is (nil? result)))))

(deftest ws-event-handler-move-test
  (testing "Handle :xiangqi/move event with from/to coordinates"
    (let [reply-called (atom false)
          reply-fn (fn [result]
                     (reset! reply-called true))
          result (api/ws-event-handler nil {:id :xiangqi/move :?data {:from [9 0] :to [8 0]} :?reply-fn reply-fn})]
      (is (not (nil? result)))
      (is @reply-called))))

(deftest ws-event-handler-unknown-test
  (testing "Return nil for unhandled event IDs"
    (let [result (api/ws-event-handler nil {:id :unknown/event :?data {}})]
      (is (nil? result)))))
