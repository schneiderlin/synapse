(ns dev
  (:require
   [powerblog.core :as blog]
   [powerpack.dev :as dev]))

(defmethod dev/configure! :default []
  blog/config)

(comment
  (dev/start)
  (dev/stop)
  (dev/reset)

  (def app (dev/get-app))
  (require '[datomic.api :as d])
  (def db (d/db (:datomic/conn app)))

  (->> (d/entity db [:page/uri "/"]))
  (->> (d/entity db [:page/uri "/blog-posts/first-post/"])
       :blog-post/author
       (into {}))
  
  (require '[clojure.java.basis :as basis])
  (basis/current-basis)

  :rcf)
