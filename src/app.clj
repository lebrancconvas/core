(ns app
  (:require [clojure.edn :as edn]
            [clojure.string :as str]
            [utils.files :as files]
            [utils.core :refer [safe-merge safe-get]]
            [gdx.app :refer [->application-listener]]
            [gdx.backends.lwjgl3 :as lwjgl3]
            [gdx.graphics.color :as color]
            [gdx.utils.screen-utils :as screen-utils]
            [core.component :as component]
            [core.context :as ctx]))

(def state (atom nil))

(defn- require-all-components! []
  (doseq [file (files/recursively-search "src/components/" #{"clj"})
          :let [ns (-> file
                       (str/replace "src/" "")
                       (str/replace ".clj" "")
                       (str/replace "/" ".")
                       symbol)]]
    (when-not (find-ns ns)
      (require ns))))

; screens require vis-ui / properties (map-editor, property editor uses properties)
(defn- context-create-order [[k _]]
  (if (= k :context/screens) 1 0))

(defn- ->application [context]
  (->application-listener
   :create (fn []
             (require-all-components!)
             (->> context
                  (sort-by context-create-order)
                  (component/create-into (assoc (ctx/->Context) :context/state state))
                  ctx/init-first-screen
                  (reset! state)))

   :dispose (fn []
              (run! component/destroy @state))

   :render (fn []
             (screen-utils/clear color/black)
             (-> @state
                 ctx/current-screen
                 (component/render! state)))

   :resize (fn [w h]
             (ctx/update-viewports @state w h))))

(defn- load-raw-properties [file]
  (let [values (-> file slurp edn/read-string)]
    (assert (apply distinct? (map :property/id values)))
    (zipmap (map :property/id values) values)))

(defn -main [& [file]]
  (let [properties (load-raw-properties file)
        {:keys [app/context app/lwjgl3]} (safe-get properties :app/core)
        context (update context
                        :context/properties
                        safe-merge
                        {:file file
                         :properties properties})]
    (lwjgl3/->application (->application context)
                          (lwjgl3/->configuration lwjgl3))))
