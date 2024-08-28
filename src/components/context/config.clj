(ns components.context.config
  (:require [core.component :refer [defcomponent] :as component]))

(defcomponent :context/config
  {:data :some
   :let {:keys [tag configs]}}
  (component/create [_ _ctx]
    (get configs tag)))
