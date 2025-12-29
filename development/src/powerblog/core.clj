(ns powerblog.core
  (:require
   [datomic.api :as d]
   [powerpack.markdown :as md]
   [powerblog.static.with-replicant :as with-replicant]))


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
  (let [{:keys [uri]} context] 
    (or
     ;; static pages. 添加新的 page 需要修改 content/static-pages.edn
     ;; 这样才会把 uri ingest 到数据库. 否则会 404
     (case uri
       "/test" [:h1 "hello"]
       "/with-replicant" with-replicant/page
       nil)
     (case (:page/kind page)
       :page.kind/frontpage (render-frontpage context page)
       :page.kind/blog-post (render-blog-post context page)
       :page.kind/article (render-article context page)))))

;; 这里面的内容修改了, 需要去 dev namespace reset 才能生效
(def config
  {:site/title "The Powerblog"
   :datomic/schema-file "bases/web-app/resources/web-app/schema.edn" 
   :powerpack/port 8000
   :powerpack/log-level :debug
   :powerpack/render-page #'render-page
   :powerpack/create-ingest-tx #'create-tx
   :powerpack/source-dirs ["bases/web-app/src"]
   :powerpack/resource-dirs ["bases/web-app/resources"] 
   ;; 注意这里不需要 bases/web-app/resources 前缀, 因为 optimus 是通过 io/resource 加载资源的, polylith 已经把各个项目的 resources 目录加到 classpath 了
   ;; 这个加载不到应该至少有个 warning 之类的东西
   :optimus/bundles {"test.js"
                     {:public-dir "public"
                      :paths ["/js/test.js"]}}})

(comment
  (require '[powerpack.assets :as pa])
  (pa/load-bundles (assoc config
                          :optimus/bundles {"test.js"
                                            {:public-dir "wrong"
                                             :paths ["/js/test.js"]}})) 
  :rcf)

