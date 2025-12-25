(ns com.zihao.replicant-main.replicant.hash-router
  (:require
   [domkm.silk :as silk] 
   [lambdaisland.uri :as uri]))

(defn essentially-same? [l1 l2]
  (and (= (:location/page-id l1) (:location/page-id l2))
       (= (not-empty (:location/params l1))
          (not-empty (:location/params l2)))
       (= (not-empty (:location/query-params l1))
          (not-empty (:location/query-params l2)))))

(defn find-target-href [e]
  (some-> e .-target
          (.closest "a")
          (.getAttribute "href")))

(defn url->location 
  "Parse location from hash URL (e.g., '#/home' or '/home' from hash).
     Accepts either a full hash string like '#/home' or just the path '/home'."
  [routes url-or-hash] 
  (let [hash-str (if (string? url-or-hash)
                   (if (.startsWith url-or-hash "#")
                     (subs url-or-hash 1) ; Remove leading '#'
                     url-or-hash)
                   url-or-hash)
        uri (uri/uri hash-str)]
    (when-let [arrived (silk/arrive routes (:path uri))]
      (let [query-params (uri/query-map uri)
            hash-params (some-> uri :fragment uri/query-string->map)]
        (cond-> {:location/page-id (:domkm.silk/name arrived)
                 :location/params (dissoc arrived
                                          :domkm.silk/name
                                          :domkm.silk/pattern
                                          :domkm.silk/routes
                                          :domkm.silk/url)}
          (seq query-params) (assoc :location/query-params query-params)
          (seq hash-params) (assoc :location/hash-params hash-params))))))

(defn location->url 
  "Generate hash URL from location (e.g., '#/home')."
  [routes {:location/keys [page-id params query-params hash-params]}]
  (let [path (silk/depart routes page-id params)
        url (cond-> path
              (seq query-params)
              (str "?" (uri/map->query-string query-params))
              (seq hash-params)
              (str "#" (uri/map->query-string hash-params)))]
    (str "#" url)))

(defn navigate! 
  "Navigate using hash-based routing (updates window.location.hash)."
  [{:keys [store execute-actions routes get-location-load-actions] :as system} new-location]
  (let [current-location (:location @store)
        hash-url (location->url routes new-location)
        load-actions (get-location-load-actions new-location)]
    (swap! store assoc :location new-location)
    (when (not (essentially-same? current-location new-location))
      #?(:cljs (set! (.-hash js/location) hash-url))
      (execute-actions system nil load-actions))))

(defn route-click 
  "Handle link clicks for hash-based routing."
  [e {:keys [routes] :as system}]
  (let [href (find-target-href e)]
    (when-let [location (url->location routes href)]
      (.preventDefault e)
      (navigate! system location))))

