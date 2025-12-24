(ns com.zihao.xiangqi.router)

(def routes
  [[:pages/xiangqi [["xiangqi"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    #_#_:pages/xiangqi [[:data/query {:query/kind :query/xiangqi
                                       :query/data {:page (query/get-page location)}}]]
    nil))
