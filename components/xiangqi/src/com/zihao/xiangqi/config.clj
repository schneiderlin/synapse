(ns com.zihao.xiangqi.config
  (:require
   [clojure.java.io :as io]
   [aero.core :refer [read-config]]))

(def config (read-config (io/resource "xiangqi/config.edn")))

(def engine-path (:engine-path config))
