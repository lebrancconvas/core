(ns components.entity.line-render
  (:require [core.component :as component :refer [defcomponent]]
            [core.graphics :as g]))

(defcomponent :entity/line-render
  {:let {:keys [thick? end color]}}
  (component/render-default [_ entity* g _ctx]
    (let [position (:position entity*)]
      (if thick?
        (g/with-shape-line-width g 4 #(g/draw-line g position end color))
        (g/draw-line g position end color)))))

(defcomponent :tx/line-render
  (component/do! [[_ {:keys [start end duration color thick?]}] _ctx]
    [[:tx/create
      {:position start
       :width 0.5
       :height 0.5
       :z-order :z-order/effect}
      #:entity {:line-render {:thick? thick? :end end :color color}
                :delete-after-duration duration}]]))
