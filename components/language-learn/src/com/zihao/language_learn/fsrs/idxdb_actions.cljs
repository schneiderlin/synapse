(ns com.zihao.language-learn.fsrs.idxdb-actions
  (:require
   [com.zihao.language-learn.fsrs.common :refer [prefix]]
   [com.zihao.language-learn.fsrs.indexeddb :as db]))

;; Helper: load due cards from IndexedDB
;; Similar to query-backend but uses IndexedDB directly
(defn load-due-cards* [{:keys [interpolate execute-actions store] :as system} & [{:keys [on-success]}]]
  (swap! store assoc-in [prefix :loading?] true)
  (-> (db/due-card-ids)
      (.then (fn [ids] (db/get-cards ids)))
      (.then (fn [cards]
               (when on-success
                 (let [actions (if (fn? on-success)
                                 (on-success cards)
                                 (interpolate cards on-success))]
                   (execute-actions system nil actions)))))))

;; Helper: repeat card using IndexedDB
;; Similar to issue-command but uses IndexedDB directly
(defn repeat-card* [{:keys [execute-actions] :as system} {:keys [id rating]} & [{:keys [on-success]}]]
  (-> (db/repeat-card! id rating)
      (.then (fn [_result]
               (when on-success
                 (let [actions (if (fn? on-success)
                                 (on-success _result)
                                 on-success)]
                   (execute-actions system nil actions)))))))

;; Action handlers that return effects
;; These use :data/idxb-query and :data/idxb-command effects

(defn load-due-cards []
  [[:data/idxb-query
    {:on-success (fn [cards]
                   [[:store/assoc-in [prefix :due-cards] cards]
                    [:store/assoc-in [prefix :current-card-index] 0]
                    [:store/assoc-in [prefix :show-back] false]
                    [:store/assoc-in [prefix :loading?] false]])}]])

(defn repeat-card [store {:keys [id rating]}]
  [[:data/idxb-command
    {:id id :rating rating}
    {:on-success (fn [_result]
                   (let [current-index (get-in @store [prefix :current-card-index])
                         due-cards (get-in @store [prefix :due-cards])
                         next-index (inc current-index)]
                     (if (< next-index (count due-cards))
                       ;; Move to next card
                       [[:store/assoc-in [prefix :current-card-index] next-index]
                        [:store/assoc-in [prefix :show-back] false]]
                       ;; All cards reviewed, reload
                       [[:data/idxb-query
                         {:on-success (fn [cards]
                                        [[:store/assoc-in [prefix :due-cards] cards]
                                         [:store/assoc-in [prefix :current-card-index] 0]
                                         [:store/assoc-in [prefix :show-back] false]
                                         [:store/assoc-in [prefix :loading?] false]])}]])))}]])

(defn show-answer []
  [[:store/assoc-in [prefix :show-back] true]])

;; Import cards from EDN file
(defn import-edn-file []
  [[:data/choose-file {}
    {:on-success [[:data/import-edn-file :event/file]]}]])

;; Extension function for make-execute-f
;; Handles :data/idxb-query and :data/idxb-command effects
(defn execute-effect [system _e action args]
  (case action
    :data/idxb-query (apply load-due-cards* system args)
    :data/idxb-command (apply repeat-card* system (first args) (rest args))
    :data/import-edn-file (let [file (first args)
                                {:keys [store]} system]
                            (swap! store assoc-in [prefix :loading?] true)
                            (-> (js/Promise.
                                 (fn [resolve reject]
                                   (let [reader (js/FileReader.)]
                                     (set! (.-onload reader)
                                           (fn [e]
                                             (resolve (.-result (.-target e)))))
                                     (set! (.-onerror reader) reject)
                                     (.readAsText reader file))))
                                (.then (fn [edn-content]
                                         (db/import-cards-from-edn edn-content)))
                                (.catch (fn [error]
                                          (swap! store assoc-in [prefix :loading?] false)
                                          (swap! store assoc-in [prefix :import-error?] true)
                                          (swap! store assoc-in [prefix :import-success?] false)
                                          (js/console.error "Import error:" error)
                                          ;; Clear error message after 5 seconds
                                          (js/setTimeout
                                           (fn []
                                             (swap! store assoc-in [prefix :import-error?] false))
                                           5000)))))
    nil))

;; Action handler that routes actions to effects
(defn execute-action [{:keys [store]} _e action args]
  (case action
    :card/load-due-cards (load-due-cards)
    :card/repeat-card (repeat-card store args)
    :flashcard/show-answer (show-answer)
    :card/import-edn-file (import-edn-file)
    nil))
