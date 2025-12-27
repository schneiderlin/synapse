(ns com.zihao.baml-client.core
  (:require
   [libpython-clj2.python :as py]))

(defn reload []
  (let [importlib (py/import-module "importlib")
        sys (py/import-module "sys")
        modules (py/py.- sys "modules")
        ;; sync_client 里面只是一个 shell, 真正是交给 runtime 执行的
        ;; runtime 构造又是根据 inlinebaml 的, 所以 inlinebaml, runtime 都需要重新加载
        modules-to-reload (reverse ["baml_client"
                                    "baml_client.async_client"
                                    "baml_client.sync_client"
                                    "baml_client.runtime"
                                    "baml_client.parser"
                                    "baml_client.type_builder"
                                    "baml_client.stream_types"
                                    "baml_client.types"
                                    "baml_client.globals"
                                    "baml_client.inlinedbaml"])]
    (doseq [module-name modules-to-reload]
      (when (get modules module-name)
        (py/py. importlib "reload" (get modules module-name))))))

(comment
  (reload)
  :rcf)
