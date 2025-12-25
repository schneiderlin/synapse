(ns com.zihao.replicant-main.replicant.main
  (:require 
   [replicant.dom :as r]
   [replicant.alias :as alias]
  ;;  [com.zihao.replicant-main.replicant.utils :refer [interpolate]]
   [com.zihao.replicant-main.replicant.ws-client :as ws-client] 
   [clojure.core.async :as async]
   [com.zihao.replicant-main.replicant.timer :as timer] 
   [com.zihao.replicant-main.replicant.router :as router]
   [com.zihao.replicant-main.replicant.hash-router :as hash-router]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)
        hash-router? (-> attrs :replicant/alias-data :hash-router?)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href ((if hash-router? hash-router/location->url router/location->url)
                              routes
                              (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)

(defn get-current-location [routes hash-router?]
  (if hash-router?
    (let [hash (.-hash js/location)]
      (if (or (nil? hash) (= hash ""))
        (hash-router/url->location routes "/")
        (hash-router/url->location routes hash)))
    (->> js/location.href
         (router/url->location routes))))

(defn init-ws-client [_ _]
  (ws-client/make-ws-client))

(defn init-ws-handler [_ {:keys [ws-client]}]
  (let [stop-ch (async/chan)]
    (ws-client/ws-handler stop-ch ws-client)
    stop-ch))

(defn halt-ws-handler [_ stop-ch]
  (async/put! stop-ch :stop))

(defn init-el [_ el]
  (js/document.getElementById el))

(defn init-render-loop [render {:keys [store el execute-actions routes interpolate get-location-load-actions hash-router?] :as system}] 
  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (render state) {:alias-data {:routes routes :hash-router? hash-router?}})))

  (add-watch
   timer/!timers :timers
   (fn [_ _ old-timers new-timers]
     (when (not= (keys old-timers) (keys new-timers))
       #_(timer/start-timer store))))

  (r/set-dispatch!
   (fn [{:keys [replicant/dom-event]} actions]
     (->> actions
          (interpolate dom-event)
          (execute-actions system dom-event))))
  
  (js/document.body.addEventListener
   "click"
   (if hash-router?
     #(hash-router/route-click % system)
     #(router/route-click % system)))

  (when hash-router?
    ;; Listen for hash changes (browser back/forward buttons)
    (js/window.addEventListener
     "hashchange"
     (fn [_]
       (let [hash (.-hash js/location)
             new-location (if (or (nil? hash) (= hash ""))
                            (hash-router/url->location routes "/")
                            (hash-router/url->location routes hash))]
         (when new-location
           (let [load-actions (get-location-load-actions new-location)]
             (swap! store assoc :location new-location)
             (execute-actions system nil load-actions)))))))

  (swap! store assoc
         :app/started-at (js/Date.)
         :location (get-current-location routes hash-router?)))





