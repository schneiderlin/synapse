(ns com.zihao.language-learn.lingq.db
  (:require
   [com.zihao.language-learn.fsrs.db :as fsrs-db] 
   [datalevin.core :as d]))

(def conn fsrs-db/conn)

(defn stability->rating [stability]
  (cond
    (< stability 0.5) 1
    (< stability 1.0) 2
    (< stability 2.0) 3
    (< stability 3.0) 4
    (< stability 4.0) 5
    :else "known"))

(comment
  (stability->rating 5.8)
  :rcf)

(defn- get-word-infos []
  (let [results (-> (d/q '[:find ?word ?stability ?difficulty
                           :where
                           [?e :card/id-word ?word]
                           [?e :fsrs/stability ?stability]
                           [?e :fsrs/difficulty ?difficulty]]
                         (d/db conn)))]
    results))

(comment
  (get-word-infos)
  :rcf)

(defn get-word-ratings []
  (let [results (get-word-infos)]
    (into {} (map (fn [[word stability difficulty]]
                    [word (stability->rating stability)]) results))))

(comment
  (-> (get-word-ratings)
      (get "di"))
  :rcf)


