(ns com.zihao.baml-client.mar
  (:require
   [martian.core :as martian]
   [martian.clj-http :as http]))

(def m (http/bootstrap-openapi "http://localhost:2024/openapi.json"))

(comment
  (martian/explore m)

  (martian/explore m :extract-resume)

  (martian/request-for m :extract-resume)
  (martian/response-for m :extract-resume)
  :rcf)
