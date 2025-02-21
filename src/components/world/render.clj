(ns components.world.render
  (:require [gdx.graphics :as graphics]
            [gdx.graphics.color :as color]
            [utils.core :refer [->tile]]
            [core.context :as ctx]
            [components.world.raycaster :as raycaster]))

(def ^:private explored-tile-color
  (graphics/->color 0.5 0.5 0.5 1))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- ->tile-color-setter [light-cache light-position raycaster explored-tile-corners]
  (fn tile-color-setter [_color x y]
    (let [position [(int x) (int y)]
          explored? (get @explored-tile-corners position) ; TODO needs int call ?
          base-color (if explored? explored-tile-color color/black)
          cache-entry (get @light-cache position :not-found)
          blocked? (if (= cache-entry :not-found)
                     (let [blocked? (raycaster/ray-blocked? raycaster light-position position)]
                       (swap! light-cache assoc position blocked?)
                       blocked?)
                     cache-entry)]
      #_(when @do-once
          (swap! ray-positions conj position))
      (if blocked?
        (if see-all-tiles? color/white base-color)
        (do (when-not explored?
              (swap! explored-tile-corners assoc (->tile position) true))
            color/white)))))

(extend-type core.context.Context
  core.context/WorldTiledMap
  (render-map [{:keys [world/tiled-map] :as ctx} light-position]
    (ctx/render-tiled-map ctx
                          tiled-map
                          (->tile-color-setter (atom nil)
                                               light-position
                                               (:world/raycaster ctx)
                                               (:world/explored-tile-corners ctx)))
    #_(reset! do-once false)))
