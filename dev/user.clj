(ns user
  (:require [clj-reload.core :as reload]))

(reload/init
 {:dirs ["src" "dev" "test"]
  :no-reload '#{user}})

(defn reload []
  (let [res (reload/reload)
        cnt (count (:loaded res))]
    (str "Reloaded: " cnt)))

(comment
  (reload))
