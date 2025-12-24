(ns com.zihao.replicant-main.replicant.main
  (:require 
   [replicant.dom :as r]
   [replicant.alias :as alias]
  ;;  [com.zihao.replicant-main.replicant.utils :refer [interpolate]]
   [com.zihao.replicant-main.replicant.ws-client :as ws-client] 
   [clojure.core.async :as async]
   [com.zihao.replicant-main.replicant.timer :as timer] 
   [com.zihao.replicant-main.replicant.router :as router]))

(defn routing-anchor [attrs children]
  (let [routes (-> attrs :replicant/alias-data :routes)]
    (into [:a (cond-> attrs
                (:ui/location attrs)
                (assoc :href (router/location->url routes
                                                   (:ui/location attrs))))]
          children)))

(alias/register! :ui/a routing-anchor)

(defn get-current-location [routes]
  (->> js/location.href
       (router/url->location routes)))

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

(defn init-render-loop [render {:keys [store el execute-actions routes interpolate] :as system}] 
  (add-watch
   store ::render
   (fn [_ _ _ state]
     (r/render el (render state) {:alias-data {:routes routes}})))

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
   #(router/route-click % system))

  (swap! store assoc
         :app/started-at (js/Date.)
         :location (get-current-location routes)))





