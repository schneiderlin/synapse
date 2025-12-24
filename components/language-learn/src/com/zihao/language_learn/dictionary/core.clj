(ns com.zihao.language-learn.dictionary.core
  (:require 
   [clojure.string :as str]
   [clj-http.client :as http]
   [hickory.core :as h]
   [hickory.select :as s]))

(defn get-translations [word]
  (let [res (http/get (str "https://en.glosbe.com/id/en/" word))
        parsed (h/parse (:body res))
        translations (-> (s/select
                          (s/descendant
                           (s/nth-of-type 1 :section)
                           (s/tag :div)
                           (s/tag :div)
                           (s/tag :ul)
                           (s/and
                            (s/tag :li)
                            (s/attr "data-element" #(= % "translation")))
                           (s/descendant (s/tag :div)
                                         (s/tag :h3)))
                          (h/as-hickory parsed)))
        words (mapv (comp str/trim first :content) translations)]
    words))

(comment
  (get-translations "anjing")
  (get-translations "mesum")

  ;; 获取基本信息
  (def res (http/get "https://en.glosbe.com/id/en/anjing"))
  (def parsed (h/parse (:body res)))
  

  

  ;; 获取例子
  (def res (http/get "https://en.glosbe.com/id/en/anjing/fragment/tmem?page=1&mode=MUST&stem=true&includedAuthors=&excludedAuthors="))
  (:body res)
  (def parsed (h/parse (:body res)))
  (def hiccup (h/as-hiccup parsed))

  :rcf)

