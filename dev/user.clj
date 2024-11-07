(ns user
  (:require
   [clj-reload.core :as reload]))

(reload/init
 {:dirs ["src" "dev" "test"]
  :no-reload '#{user}})

(defn reload [] (reload/reload))
