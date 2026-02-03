(ns com.zihao.playground-rama.path
  (:require
   [com.rpl.rama.path :as path]
   [com.rpl.rama :as r]
   [com.rpl.rama.test :as rtest]))

(comment
  (def data {"a0" {:a1 [9 3 6]
                   :b1 [0 8]}
             "b0" {:c1 ["x y"]}})

  ;; 类似 clojure 的 get-in
  (path/select
   ["a0" :a1 1]
   data)

  ;; 可以同时 focus 多个, 不像 get-in
  ;; visualize 的时候可以这样想, a0 的时候还是一个 focus pointer, 到了 MAP-VALS 就会分裂出 count 个 focus
  ;; 所以返回的结果是个 list
  (path/select
   ["a0" path/MAP-VALS]
   data)

  ;; 分裂了之后还能继续分裂, ALL 就是同时 focus 到当前的每一个 value
  (path/select
   ["a0" path/MAP-VALS path/ALL]
   data)

  ;; 还可以用匿名函数 filter
  (path/select
   ["a0" path/MAP-VALS path/ALL #(< % 7)]
   data)

  ;; 用 test-pstate 可以测试 dataflow 和 rama 的关联
  #_:clj-kondo/ignore
  (with-open [ps (rtest/create-test-pstate {String Long})]
    (rtest/test-pstate-transform ["hello" (path/termval 2)] ps)
    (?<-
     (r/local-select> ["hello"] ps :> *out)
     (println *out)))

   ;; transform 类似 put
  #_:clj-kondo/ignore
  (with-open [ps (rtest/create-test-pstate Object)]
    (rtest/test-pstate-transform ["a" (path/termval 0)] ps)
    (rtest/test-pstate-transform ["b" (path/termval 1)] ps)
    (?<-
     (r/local-select> [path/STAY] ps :> *initial) ;; clj-kondo/ignore - Rama binding symbol
     (println "initial: " *initial)
     ;; 注意这里是用 map-key 把 a 这个 key 换成了 c. 而不是换 value
     (r/local-transform> [(path/map-key "a") (path/termval "c")] ps)
     (r/local-select> [path/STAY] ps :> *newVal) ;; clj-kondo/ignore - Rama binding symbol
     (println "after tranform: " *newVal)))

   ;; 如果 focus 到一个还不存在的 value, 那么 transform 就类似 add
  #_:clj-kondo/ignore
  (with-open [ps (rtest/create-test-pstate Object)]
    (rtest/test-pstate-transform [path/NIL->SET (path/termval "a")] ps)
    (?<-
     (r/local-select> [path/STAY] ps :> *initial) ;; clj-kondo/ignore - Rama binding symbol
     (println "initial: " *initial)
     (r/local-transform> [path/NIL->SET (path/termval "f")] ps)
     (r/local-select> [path/STAY] ps :> *after-first) ;; clj-kondo/ignore - Rama binding symbol
     (println "after 1: " *after-first)))

   ;; list
  #_:clj-kondo/ignore
  (with-open [ps (rtest/create-test-pstate Object)]
    (rtest/test-pstate-transform [path/AFTER-ELEM (path/termval "origin")] ps)
    (?<-
     (r/local-select> [path/STAY] ps :> *initial) ;; clj-kondo/ignore - Rama binding symbol
     (println "initial: " *initial)
     (r/local-transform> [path/AFTER-ELEM (path/termval "append")] ps)
     (r/local-select> [path/STAY] ps :> *after1) ;; clj-kondo/ignore - Rama binding symbol
     (println "after 1: " *after1)
     (r/local-transform> [path/BEFORE-ELEM (path/termval "prepend")] ps)
     (r/local-select> [path/STAY] ps :> *after2) ;; clj-kondo/ignore - Rama binding symbol
     (println "after 2: " *after2)
     (r/local-transform> [(path/before-index 2) (path/termval "insert 2")] ps)
     (r/local-select> [path/STAY] ps :> *after3) ;; clj-kondo/ignore - Rama binding symbol
     (println "after 3: " *after3)))

  ;; 高级的 filter, 用 nested path 判断里面有没有内容, 没有就过滤掉
  (path/select
   [path/MAP-VALS
    ;; 这是一个 filter, 是不会把当前的 focus 带到里面的, 只是用里面的东西判断.
    (path/selected? path/MAP-VALS path/ALL #(= % 8))]
   data)

   ;; 把其中的某段换了
  #_:clj-kondo/ignore
  (with-open [ps (rtest/create-test-pstate Object)]
    (?<-
     (r/local-transform> [path/AFTER-ELEM (path/termval 1)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 2)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 3)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 4)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 5)] ps)
     (r/local-transform> [(path/srange 1 4) (path/termval [1])] ps)
     (r/local-select> [path/STAY] ps :> *v) ;; clj-kondo/ignore - Rama binding symbol
     (println "initial: " *v)))

   ;; 用 term 类似 update 
  (with-open [ps (rtest/create-test-pstate Object)] 
    (?<-
     (r/local-transform> [path/AFTER-ELEM (path/termval 1)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 2)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 3)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 4)] ps)
     (r/local-transform> [path/AFTER-ELEM (path/termval 5)] ps)
     (r/local-transform> [(path/srange 1 4) (path/term inc)] ps)
     (r/local-select> [path/STAY] ps :> *v)
     (println "initial: " *v)))

  :rcf)

