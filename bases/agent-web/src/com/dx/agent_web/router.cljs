(ns com.dx.agent-web.router
  (:require
   [domkm.silk :as silk]))

(def routes
  (silk/routes
   [[:pages/home [["home"]]]]))

(defn get-location-load-actions [_]
  nil)
