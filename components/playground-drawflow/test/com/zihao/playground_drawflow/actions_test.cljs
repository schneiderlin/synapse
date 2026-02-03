(ns com.zihao.playground-drawflow.actions-test
  (:require [clojure.test :refer [deftest testing is]]
            [com.zihao.playground-drawflow.actions :as actions]))

;; Node actions tests
(deftest execute-action-add-node-test
  (testing "Routes to add-node action"
    (let [store (atom {})
          system {:store store}
          action :canvas/add-node
          args [{:id "test-node" :type "test"}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-update-node-test
  (testing "Routes to update-node action"
    (let [store (atom {})
          system {:store store}
          action :canvas/update-node
          args ["test-node" {:type "updated"}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-delete-node-test
  (testing "Routes to delete-node action"
    (let [store (atom {})
          system {:store store}
          action :canvas/delete-node
          args ["test-node"]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Edge actions tests
(deftest execute-action-add-edge-test
  (testing "Routes to add-edge action"
    (let [store (atom {})
          system {:store store}
          action :canvas/add-edge
          args [{:source "node1" :target "node2"}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-delete-edge-test
  (testing "Routes to delete-edge action"
    (let [store (atom {})
          system {:store store}
          action :canvas/delete-edge
          args ["edge-id"]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Selection actions tests
(deftest execute-action-select-test
  (testing "Routes to select action"
    (let [store (atom {})
          system {:store store}
          action :canvas/select
          args ["test-node"]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Viewport actions tests
(deftest execute-action-pan-test
  (testing "Routes to pan action"
    (let [store (atom {})
          system {:store store}
          action :canvas/pan
          args [{:x 10 :y 10}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-zoom-test
  (testing "Routes to zoom action"
    (let [store (atom {})
          system {:store store}
          action :canvas/zoom
          args [1.1]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-set-viewport-test
  (testing "Routes to set-viewport action"
    (let [store (atom {})
          system {:store store}
          action :canvas/set-viewport
          args [{:x 0 :y 0 :scale 1}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Dragging actions tests
(deftest execute-action-node-mouse-down-test
  (testing "Routes to node-mouse-down action"
    (let [store (atom {})
          system {:store store}
          action :canvas/node-mouse-down
          args ["test-node" {:x 0 :y 0}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-mouse-move-test
  (testing "Routes to mouse-move action"
    (let [store (atom {})
          system {:store store}
          action :canvas/mouse-move
          args [{:x 10 :y 10}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-mouse-up-test
  (testing "Routes to mouse-up action"
    (let [store (atom {})
          system {:store store}
          action :canvas/mouse-up
          args []
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Connection actions tests
(deftest execute-action-handle-mouse-down-test
  (testing "Routes to handle-mouse-down action"
    (let [store (atom {})
          system {:store store}
          action :canvas/handle-mouse-down
          args ["test-node" "handle-id" {:x 0 :y 0}]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

(deftest execute-action-complete-connection-test
  (testing "Routes to complete-connection action"
    (let [store (atom {})
          system {:store store}
          action :canvas/complete-connection
          args ["source-handle" "target-handle"]
          result (actions/execute-action system nil action args)]
      (is (some? result)))))

;; Unknown action test
(deftest execute-action-unknown-test
  (testing "Returns nil for unknown action"
    (let [store (atom {})
          system {:store store}
          action :unknown/action
          args [:test-args]
          result (actions/execute-action system nil action args)]
      (is (nil? result)))))
