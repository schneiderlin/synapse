(ns com.zihao.language-learn.fsrs.db
  (:require
   [clojure.string :as str]
   [clojure.java.io :as io]
   [com.zihao.language-learn.fsrs.core :as core]
   [com.zihao.language-learn.fsrs.template :as template]
   [datalevin.core :as d]))

(def db-uri "fsrs.db")
(def schema
  {:card/title {:db/valueType :db.type/string
                :db/cardinality :db.cardinality/one
                :db/unique :db.unique/identity}
   :card/id-word {:db/valueType :db.type/string
                  :db/unique :db.unique/identity}
   :card/front {:db/cardinality :db.cardinality/one}
   :card/back {:db/cardinality :db.cardinality/one}
   :fsrs/lapses {:db/valueType :db.type/long}
   :fsrs/stability {:db/valueType :db.type/double}
   :fsrs/difficulty {:db/valueType :db.type/double}
   :fsrs/last-repeat {:db/valueType :db.type/instant}
   :fsrs/reps {:db/valueType :db.type/long}
   :fsrs/state {:db/valueType :db.type/keyword}
   :fsrs/due {:db/valueType :db.type/instant}
   :fsrs/elapsed-days {:db/valueType :db.type/long}
   :fsrs/scheduled-days {:db/valueType :db.type/long}})

(def conn (d/get-conn db-uri schema))

(defn save-card! [card]
  (d/transact! conn [card]))

(comment 
  (require '[com.zihao.language-learn.fsrs.template :as template])
  (let [card (template/bhs-indo-card "anjing" "dog")]
    (save-card! card))
  :rcf)

(defn due-card-ids []
  (let [now (java.util.Date.)]
    (d/q '[:find [?e ...]
           :in $ ?now
           :where
           [?e :fsrs/due ?due]
           [(< ?due ?now)]]
         (d/db conn) now)))

(comment
  (due-card-ids)
  :rcf)

(defn repeat-card! [id rating]
  (let [card (d/pull @conn '[*] id)]
    (d/transact! conn [(core/repeat-card card rating)])))

(comment
  (repeat-card! 1 :good)
  (d/pull @conn '[*] 1)

  (d/pull-many @conn '[*] [1 2])
  :rcf)

(defn repeat-word! [id-word rating]
  (let [card (d/pull @conn '[*] [:card/id-word id-word])]
    (d/transact! conn [(core/repeat-card card rating)])))

(comment
  (d/pull @conn '[*] [:card/id-word "bahwa"])
  :rcf)

(defn normalize-word [word]
  (str/lower-case word))

(comment
  (normalize-word "Anjing")
  :rcf)

(defn by-id-word [id-word]
  (d/q '[:find ?e . 
         :in $ ?id-word
         :where
         [?e :card/id-word ?id-word]]
       (d/db conn) id-word))

(comment
  (d/pull @conn '[*] (by-id-word "di"))
  (by-id-word "wordld")
  :rcf)

(defn import-words-file [file]
  (let [lines (str/split-lines (slurp file))
        cards (map (fn [line]
                     (let [[id-text en-text] (str/split line #"\s+")
                           id-text (normalize-word id-text)
                           en-text (normalize-word en-text)]
                       (template/bhs-indo-card id-text en-text)))
                   lines)]
    (doseq [card cards]
      (when-not (by-id-word (:card/id-word card))
        (save-card! card)))))

(comment
  (import-words-file (io/file (io/resource "data/words.txt")))

  (-> (d/q '[:find ?word 
         :where
         [?e :card/id-word ?word]]
       (d/db conn))
      count) 
  
   (by-id-word "Halo")
  (d/transact! conn [(assoc (d/pull @conn '[*] 151)
                            :card/id-word "Halo")])
  :rcf)

;; 象棋测试
(comment
  (template/xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w"
                         "a0a1")
  
  (save-card! (assoc (template/xiangqi-card "rnbakabnr/9/1c5c1/p1p1p1p1p/9/9/P1P1P1P1P/1C5C1/9/RNBAKABNR w"
                                     "h2e2")
                     :db/id 149))

  (d/q '[:find [?e ...]
         :where
         [?e :card/fen]]
       (d/db conn))
  
  (d/pull @conn '[:db/id :fsrs/due] 149)
  (d/transact! conn [(assoc (d/pull @conn '[*] 149) 
                            :fsrs/due (java.util.Date.))])
  
  :rcf)

