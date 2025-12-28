(ns powerblog.core
  (:require
   [datomic.api :as d]
   [powerpack.markdown :as md]))


(comment
  (require '[powerpack.dev :as dev])
  (def app (dev/get-app))
  (def db (d/db (:datomic/conn app)))
  :rcf)

(defn get-page-kind [file-name]
  (cond
    (re-find #"^blog-posts/" file-name)
    :page.kind/blog-post

    (re-find #"^index\.md" file-name)
    :page.kind/frontpage

    (re-find #"\.md$" file-name)
    :page.kind/article))

(defn create-tx [file-name txes]
  (let [kind (get-page-kind file-name)]
    (for [tx txes]
      (cond-> tx
        (and (:page/uri tx) kind)
        (assoc :page/kind kind)))))

(defn get-blog-posts [db]
  (->> (d/q '[:find [?e ...]
              :where
              [?e :blog-post/author]]
            db)
       (map #(d/entity db %))))

(defn get-posts-by-tag [db tag]
  (->> (d/q '[:find [?e ...]
              :in $ ?tag
              :where
              [?e :blog-post/tags ?tag]]
            db tag)
       (map #(d/entity db %))))

(comment
  (get-posts-by-tag db :clojure) 
  :rcf)

(defn layout [{:keys [title]} & content]
  [:html
   [:head
    (when title [:title title])]
   [:body
    content]])

(def header
  [:header [:a {:href "/"} "首页"]])

(defn render-frontpage [context page]
  (layout
   {:title "Jan 博客"}
   (md/render-html (:page/body page))
   [:h2 "Blog posts"]
   [:ul
    (for [blog-post (get-blog-posts (:app/db context))]
      [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]
   [:h2 "Songs of syx"]
   [:ul
    (for [blog-post (get-posts-by-tag (:app/db context) :songs-of-syx)]
      [:li [:a {:href (:page/uri blog-post)} (:page/title blog-post)]])]))

(defn render-article [context page]
  (layout {}
          header
          (md/render-html (:page/body page))))

(defn render-blog-post [context page]
  (render-article context page))

(defn render-page [context page]
  (case (:page/kind page)
    :page.kind/frontpage (render-frontpage context page)
    :page.kind/blog-post (render-blog-post context page)
    :page.kind/article (render-article context page)))


(def config
  {:site/title "The Powerblog"
   :datomic/schema-file "bases/web-app/resources/web-app/schema.edn" 
   :powerpack/port 8000
   :powerpack/log-level :debug
   :powerpack/render-page #'render-page
   :powerpack/create-ingest-tx #'create-tx
   :powerpack/source-dirs ["bases/web-app/src"]
   :powerpack/resource-dirs ["bases/web-app/resources"]})
