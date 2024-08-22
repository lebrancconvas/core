(ns world.mouseover-entity
  (:require [utils.core :refer [sort-by-order]]
            [api.context :as ctx :refer [mouse-on-stage-actor? world-grid line-of-sight?]]
            [api.entity :as entity]
            [api.world.grid :refer [point->entities]]))

(defn- calculate-mouseover-entity [context]
  (let [player-entity* (ctx/player-entity* context)
        hits (filter #(and (:z-order %)
                           (not= (:z-order %)
                                 :z-order/effect))
                     (map deref
                          (point->entities (world-grid context)
                                           (ctx/world-mouse-position context))))]
    (->> entity/render-order
         (sort-by-order hits :z-order)
         reverse
         (filter #(line-of-sight? context player-entity* %))
         first
         :entity/id)))

(extend-type api.context.Context
  api.context/MouseOverEntity
  (mouseover-entity* [ctx]
    (when-let [entity (::mouseover-entity ctx)]
      @entity))

  (update-mouseover-entity [ctx]
    (let [entity (if (mouse-on-stage-actor? ctx)
                   nil
                   (calculate-mouseover-entity ctx))]
      [(when-let [old-entity (::mouseover-entity ctx)]
         [:tx.entity/dissoc old-entity :entity/mouseover?])
       (when entity
         [:tx.entity/assoc entity :entity/mouseover? true])
       (fn [ctx]
         (assoc ctx ::mouseover-entity entity))])))
