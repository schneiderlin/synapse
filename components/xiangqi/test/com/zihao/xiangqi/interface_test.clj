(ns com.zihao.xiangqi.interface-test
  (:require [clojure.test :refer :all]
            [com.zihao.xiangqi.interface :as xiangqi]))

(deftest ws-event-handler-frontend-game-state-update-test
  (testing "Frontend WebSocket handler returns nil in CLJ (CLJS-only function)"
    (let [store (atom {})
          system {:replicant/store store}
          event-msg {:id :xiangqi/game-state-update :?data {:test "data"}}
          result (xiangqi/ws-event-handler-frontend system event-msg)]
      ;; In CLJ, ws-event-handler-frontend returns nil (it's a CLJS-only function)
      ;; CLJS tests would verify the actual behavior
      (is (nil? result))
      (is (= {} @store)))))

(deftest ws-event-handler-frontend-unknown-test
  (testing "Frontend WebSocket handler returns nil for unhandled event IDs"
    (let [store (atom {})
          system {:replicant/store store}
          event-msg {:id :xiangqi/unknown-event :?data {:test "data"}}
          result (xiangqi/ws-event-handler-frontend system event-msg)]
      (is (nil? result))
      (is (= {} @store)))))

(deftest chessboard-test
  (testing "Chessboard renders from initial state"
    (let [state (xiangqi/fen->state "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w")
          chessboard-result (xiangqi/chessboard state)]
      (is (vector? chessboard-result))
      (is (= :div (first chessboard-result))))))
