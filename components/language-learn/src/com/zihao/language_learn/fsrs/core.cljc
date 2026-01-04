(ns com.zihao.language-learn.fsrs.core
  (:require
   [open-spaced-repetition.cljc-fsrs.core :as fsrs])
  #?(:clj (:import
           [java.util Date])))

;; fsrs 用法
(comment
  ;; 创建一个 card
  (def card (fsrs/new-card!))

  {:again 1 ;; We got the answer wrong. Automatically means that we
   ;; have forgotten the card. This is a lapse in memory.
   :hard  2 ;; The answer was only partially correct and/or we took
   ;; too long to recall it.
   :good  3 ;; The answer was correct but we were not confident about it.
   :easy  4 ;; The answer was correct and we were confident and quick
   ;; in our recall.
   }

  ;; 复习给一个回复
  (-> card
      (fsrs/repeat-card! :good))

  ;; card 里面可以有其他字段, 不影响
  (-> (assoc card :other 1)
      (fsrs/repeat-card! :good))
  :rcf)

#?(:clj
   (defn- instant->date
     "Convert java.time.Instant to java.util.Date"
     [instant]
     (when instant
       (Date/from instant))))

#?(:clj
   (defn- date->instant [date]
     (when date
       (.toInstant date))))

(comment
  #?(:clj (date->instant (Date.)))
  :rcf)

(defn- fsrs-card->card [fsrs-card]
  (-> fsrs-card
      (merge
       {:fsrs/lapses (:lapses fsrs-card)
        :fsrs/stability (:stability fsrs-card)
        :fsrs/difficulty (:difficulty fsrs-card)
        :fsrs/last-repeat #?(:clj (instant->date (:last-repeat fsrs-card))
                             :cljs (:last-repeat fsrs-card))
        :fsrs/reps (:reps fsrs-card)
        :fsrs/state (:state fsrs-card)
        :fsrs/due #?(:clj (instant->date (:due fsrs-card))
                     :cljs (:due fsrs-card))
        :fsrs/elapsed-days (:elapsed-days fsrs-card)
        :fsrs/scheduled-days (:scheduled-days fsrs-card)})
      (dissoc :lapses :stability :difficulty :last-repeat :reps :state :due :elapsed-days :scheduled-days)))

(defn- card->fsrs-card [card]
  (let [{:fsrs/keys [lapses stability difficulty last-repeat reps state due elapsed-days scheduled-days]} card]
    (-> card
        (merge
         {:lapses lapses
          :stability stability
          :difficulty difficulty
          :last-repeat #?(:clj (date->instant last-repeat)
                          :cljs last-repeat)
          :reps reps
          :state state
          :due #?(:clj (date->instant due)
                  :cljs due)
          :elapsed-days elapsed-days
          :scheduled-days scheduled-days})
        (dissoc :fsrs/lapses :fsrs/stability :fsrs/difficulty :fsrs/last-repeat :fsrs/reps :fsrs/state :fsrs/due :fsrs/elapsed-days :fsrs/scheduled-days))))

(comment
  (-> (assoc (fsrs/new-card!) :other 1)
      fsrs-card->card
      card->fsrs-card)
  :rcf)

(defn create-card [title front back]
  (let [fsrs-card (fsrs/new-card!)
        db-card (fsrs-card->card fsrs-card)]
    (assoc db-card
           :card/title title
           :card/front front
           :card/back back)))

(defn repeat-card [card rating]
  (let [fsrs-card (card->fsrs-card card)] 
    (-> fsrs-card
        (fsrs/repeat-card! rating)
        fsrs-card->card)))

(comment
  (repeat-card (create-card "test" "front" "back") :good) 
  :rcf)
