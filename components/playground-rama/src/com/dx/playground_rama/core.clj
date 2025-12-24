(ns com.zihao.playground-rama.core
  (:require
   [com.rpl.rama :as r]
   [com.rpl.rama.path :as path]
   [com.rpl.rama.ops :as ops]
   [com.rpl.rama.aggs :as aggs]
   [com.rpl.rama.test :as rtest]))

(def sentence-id String)
(defrecord Sentence [id text])

(defn split-words [sentence]
  (re-seq #"\w+" sentence))

(comment
  (split-words "Hello World!")
  :rcf)

#_:clj-kondo/ignore
(r/defmodule ConversationModule [setup topo]
  (r/declare-depot setup "*sentenceDepot" :random)
  (let [s (r/stream-topology topo "conversationStream")]
    (r/declare-pstate s $$sentences {sentence-id Sentence})
    (r/declare-pstate s $$wordCounts {String Long})
    (r/declare-pstate s $$wordInSentences {String (r/set-schema sentence-id)})

    (r/<<sources s
                 (r/source> *sentenceDepot :> {:keys [*id *text]
                                               :as *sentence})
                 (r/local-transform> [(path/keypath *id)
                                      (path/termval *sentence)] $$sentences)
                 (ops/explode (split-words *text) :> *word)
                 (r/+compound $$wordCounts {*word (aggs/+count)})
                 (r/+compound $$wordInSentences {*word (aggs/+set-agg (:id *sentence))}))))

;; deploy 模式
(comment 
  (def manager (r/open-cluster-manager {"conductor.host" "localhost"}))

  (def depot (r/foreign-depot manager
                              (r/get-module-name ConversationModule)
                              "*sentenceDepot"))
  (r/foreign-object-info depot)
  (r/foreign-depot-partition-info depot 1)
  (r/foreign-depot-read depot 0 0 3)
  (r/foreign-depot-read depot 0 1 3)

  (r/foreign-append! depot (map->Sentence {:id "1" :text "hello world!"}))
  (r/foreign-append! depot (map->Sentence {:id "2" :text "there"}))
  (r/foreign-append! depot (map->Sentence {:id "3" :text "what are you doing in there?"}))

  (def word-count-pstate (r/foreign-pstate manager
                                           (r/get-module-name ConversationModule)
                                           "$$wordCounts"))
  (def sentences-pstate (r/foreign-pstate manager
                                          (r/get-module-name ConversationModule)
                                          "$$sentences"))
  (def word-in-sentences-pstate (r/foreign-pstate manager
                                                  (r/get-module-name ConversationModule)
                                                  "$$wordInSentences"))

  (r/foreign-select-one ["hello"] word-count-pstate)
  (r/foreign-select ["1"] sentences-pstate)
  (r/foreign-select ["there"] word-in-sentences-pstate)
  :rcf)

;; ipc 模式
(comment
  (def cluster (rtest/create-ipc))

  ;; 在 cluster 里面跑某个 module
  (rtest/launch-module! cluster ConversationModule {:tasks 1 :threads 1})
  ;; 可以实时更新, depot 里面的数据还在
  (rtest/update-module! cluster ConversationModule)
  (rtest/destroy-module! cluster "ConversationModule")

  ;; 获取 module 里面的 depot
  (def depot (r/foreign-depot cluster
                              (r/get-module-name ConversationModule)
                              "*sentenceDepot"))

  (r/foreign-append! depot (map->Sentence {:id "1" :text "hello world!"}))
  (r/foreign-append! depot (map->Sentence {:id "2" :text "there"}))
  (r/foreign-append! depot (map->Sentence {:id "3" :text "what are you doing in there?"}))

  ;; 获取 module 里面的 pstate
  (def word-count-pstate (r/foreign-pstate cluster
                                           (r/get-module-name ConversationModule)
                                           "$$wordCounts"))
  (def sentences-pstate (r/foreign-pstate cluster
                                         (r/get-module-name ConversationModule)
                                         "$$sentences"))
  (def word-in-sentences-pstate (r/foreign-pstate cluster
                                                 (r/get-module-name ConversationModule)
                                                 "$$wordInSentences"))

  (r/foreign-select-one (path/keypath "there") word-count-pstate)
  (r/foreign-select [path/ALL] sentences-pstate)
  (r/foreign-select [path/ALL] word-in-sentences-pstate)
  :rcf)
