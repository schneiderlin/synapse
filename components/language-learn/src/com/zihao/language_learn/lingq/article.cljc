(ns com.zihao.language-learn.lingq.article
  (:require
   [com.zihao.language-learn.lingq.common :refer [prefix]]
   [clojure.string :as str]))

(defn get-word-class [rating]
  (concat ["px-0.5" "py-0.5" "rounded" "text-lg"]
          (case rating
            "known" []
            1 ["cursor-pointer" "bg-orange-900" "text-white"]
            2 ["cursor-pointer" "bg-orange-800" "text-white"]
            3 ["cursor-pointer" "bg-orange-600" "text-white"]
            4 ["cursor-pointer" "bg-orange-500" "text-white"]
            5 ["cursor-pointer" "bg-orange-400" "text-white"]
            nil ["cursor-pointer" "bg-blue-600" "text-white"]
            :else ["unknown-rating"])))

(comment
  (get-word-class "known")
  :rcf)

(defn article-ui [{:keys [tokens word->rating] :as state}] 
  [:div {:style {:white-space "pre-wrap"
                 :font-size "1.25rem"
                 :line-height "1.75rem"}}
   (for [token tokens]
     (do 
       (println "token" token)
       (let [rating (word->rating (str/lower-case token))]
         (if (or (re-matches #"\s+" token)
                 (re-matches #"[.。,，!！?？;；:：“”'\"\[\]\(\)\{\}<>-]" token)
                 (re-matches #"\d+" token))
           token
           [:span {:class (get-word-class rating)
                   :on {:click
                        (if (nil? rating)
                          [[:lingq/click-unknown-word {:word (str/lower-case token)}]]
                          [[:store/assoc-in [prefix :selected-word] token]])}}
            token]))))])
