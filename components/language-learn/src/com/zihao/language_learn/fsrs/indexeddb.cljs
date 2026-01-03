(ns com.zihao.language-learn.fsrs.indexeddb
  (:require [clojure.string :as str]
            [com.zihao.language-learn.fsrs.core :as core]))

;; Database configuration
(def db-name "fsrs")
(def db-version 2)
(def store-name "cards")

;; Database connection promise (singleton)
(defonce db-promise
  (js/Promise.
   (fn [resolve reject]
     (let [req (js/indexedDB.open db-name db-version)]
       ;; On success
       (set! (.-onsuccess req)
             (fn [e]
               (resolve (.. e -target -result))))
       ;; On error
       (set! (.-onerror req)
             (fn [e]
               (reject e)))
       ;; On upgrade needed (create schema)
       (set! (.-onupgradeneeded req)
             (fn [e]
               (let [db (.. e -target -result)]
                 ;; Create store if it doesn't exist
                 (when (not (.contains (.-objectStoreNames db) store-name))
                   (let [store (.createObjectStore db store-name
                                                   #js{:keyPath "db_id"
                                                       :autoIncrement true})]
                     (.createIndex store "due" "due" #js{})
                     (.createIndex store "id_word" "id_word" #js{:unique true}))))))))))

;; Helper: convert Clojure card to JS object for IndexedDB
;; Flattens namespaced keywords to plain keys for IndexedDB
(defn- card->js [card]
  (clj->js
    (cond-> {:title (:card/title card)
             :id_word (:card/id-word card)
             :front (:card/front card)
             :back (:card/back card)
             :lapses (:fsrs/lapses card 0)
             :stability (:fsrs/stability card 0.0)
             :difficulty (:fsrs/difficulty card 5.0)
             :last-repeat (:fsrs/last-repeat card)
             :reps (:fsrs/reps card 0)
             :state (name (:fsrs/state card :new))  ; Keyword -> string for JSON
             :due (:fsrs/due card)
             :elapsed-days (:fsrs/elapsed-days card 0)
             :scheduled-days (:fsrs/scheduled-days card 0)}
      (:db/id card) (assoc :db_id (:db/id card)))))

;; Helper: convert JS object to Clojure card map
;; Restores namespaced keywords to match Datalevin schema
(defn- js->card [js-obj]
  (when js-obj
    (let [obj (js->clj js-obj :keywordize-keys true)]
      {:db/id (:db_id obj)
       :card/title (:title obj)
       :card/id-word (:id_word obj)
       :card/front (:front obj)
       :card/back (:back obj)
       :fsrs/lapses (:lapses obj)
       :fsrs/stability (:stability obj)
       :fsrs/difficulty (:difficulty obj)
       :fsrs/last-repeat (:last-repeat obj)
       :fsrs/reps (:reps obj)
       :fsrs/state (keyword (:state obj))
       :fsrs/due (:due obj)
       :fsrs/elapsed-days (:elapsed-days obj)
       :fsrs/scheduled-days (:scheduled-days obj)})))

;; Save or update a card
;; Returns promise that resolves to the :db/id of the saved card
(defn save-card! [card]
  (.then db-promise
         (fn [db]
           (js/Promise.
            (fn [resolve reject]
              (let [tx (.transaction db #js [store-name] "readwrite")
                    store (.objectStore tx store-name)
                    js-card (card->js card)
                    req (.put store js-card)]
                (set! (.-onsuccess req)
                      (fn [e]
                        ;; Return the generated :db/id
                        (resolve (.. e -target -result))))
                (set! (.-onerror req) reject)))))))

;; Get :db/id by :card/id-word
;; Returns promise that resolves to :db/id (long) or nil
(defn by-id-word [id-word]
  (.then db-promise
         (fn [db]
           (js/Promise.
            (fn [resolve reject]
              (let [tx (.transaction db #js [store-name] "readonly")
                    store (.objectStore tx store-name)
                    index (.index store "id_word")
                    req (.get index id-word)]
                (set! (.-onsuccess req)
                      (fn [e]
                        (let [result (.. e -target -result)]
                          (resolve (when result (:db/id (js->card result)))))))
                (set! (.-onerror req) reject)))))))

;; Get full card by :db/id
;; Internal helper for repeat-card! implementation
(defn- by-id [id]
  (.then db-promise
         (fn [db]
           (js/Promise.
            (fn [resolve reject]
              (let [tx (.transaction db #js [store-name] "readonly")
                    store (.objectStore tx store-name)
                    req (.get store id)]
                (set! (.-onsuccess req)
                      (fn [e]
                        (resolve (js->card (.. e -target -result)))))
                (set! (.-onerror req) reject)))))))

;; Get :db/ids of cards due for review
;; Returns promise that resolves to vector of :db/id longs
(defn due-card-ids []
  (.then db-promise
         (fn [db]
           (js/Promise.
            (fn [resolve reject]
              (let [tx (.transaction db #js [store-name] "readonly")
                    store (.objectStore tx store-name)
                    index (.index store "due")
                    now (js/Date.)
                    req (.openCursor index)
                    results (array)]
                (set! (.-onsuccess req)
                      (fn [e]
                        (let [cursor (.. e -target -result)]
                          (if cursor
                            (let [^js value (.-value cursor)
                                  due-date (.-due value)]
                              (if (< due-date now)
                                (do
                                  (.push results (.-db_id value))
                                  (.continue cursor))
                                (resolve (js->clj results))))
                            (resolve (js->clj results))))))
                (set! (.-onerror req) reject)))))))

;; Repeat card by :db/id
;; Returns promise that resolves to updated card's :db/id
(defn repeat-card! [id rating]
  (.then (by-id id)
         (fn [card]
           (if card
             (let [updated-card (core/repeat-card card rating)]
               (save-card! updated-card))
             (throw (ex-info "Card not found" {:db/id id}))))))

;; Repeat card by :card/id-word
;; Returns promise that resolves to updated card's :db/id
(defn repeat-word! [id-word rating]
  (.then (by-id-word id-word)
         (fn [id]
           (if id
             (repeat-card! id rating)
             (throw (ex-info "Card not found" {:card/id-word id-word}))))))

;; Get full cards by :db/ids
;; Helper similar to datalevin's pull-many for use in api layer
(defn get-cards [ids]
  (js/Promise.all
   (mapv by-id ids)))

(comment
  (def card 
    {:card/title "Hello"
     :card/id-word "hello"
     :card/front "Hello"
     :card/back "你好"
     :fsrs/due (js/Date.)
     :fsrs/state :new})
  
  (card->js card)

  (defn add-test-card []
    (.then (save-card! {:card/title "Hello"
                        :card/id-word "hello"
                        :card/front "Hello"
                        :card/back "你好"
                        :fsrs/due (js/Date.)
                        :fsrs/state :new})
           (fn [db-id] (js/console.log "Saved card with db/id:" db-id))))
  
  (add-test-card)

  (.then (get-cards [1])
         prn)
  :rcf)
