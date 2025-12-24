(ns com.dx.playground-drawflow.router)

(def routes
  [[:pages/playground-drawflow [["playground-drawflow"]]]])

(defn get-location-load-actions [location]
  (case (:location/page-id location)
    :pages/playground-drawflow nil
    nil))

