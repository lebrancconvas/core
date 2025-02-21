(ns components.world.debug-render
  (:require [gdx.graphics.color :as color]
            [utils.core :refer [->tile]]
            [utils.camera :as camera]
            [math.geom :as geom]
            [core.context :as ctx :refer [world-grid]]
            [core.graphics :as g]
            [core.world.grid :refer [circle->cells]]
            [components.world.potential-fields :as potential-field]))

(defn- geom-test [g ctx]
  (let [position (ctx/world-mouse-position g)
        grid (world-grid ctx)
        radius 0.8
        circle {:position position :radius radius}]
    (g/draw-circle g position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%)
                       (circle->cells grid circle))]
      (g/draw-rectangle g x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (geom/circle->outer-rectangle circle)]
      (g/draw-rectangle g x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(defn- tile-debug [g ctx]
  (let [grid (world-grid ctx)
        world-camera (ctx/world-camera ctx)
        [left-x right-x bottom-y top-y] (camera/frustum world-camera)]

    (when tile-grid?
      (g/draw-grid g (int left-x) (int bottom-y)
                   (inc (int (ctx/world-viewport-width ctx)))
                   (+ 2 (int (ctx/world-viewport-height ctx)))
                   1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles world-camera)
            :let [cell (grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (g/draw-filled-rectangle g x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (g/draw-filled-rectangle g x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance entity]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (@#'potential-field/factions-iterations faction))]
              (g/draw-filled-rectangle g x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile [g ctx]
  (when highlight-blocked-cell?
    (let [[x y] (->tile (ctx/world-mouse-position ctx))
          cell (get (world-grid ctx) [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (g/draw-rectangle g x y 1 1
                          (case (:movement @cell)
                            :air  [1 1 0 0.5]
                            :none [1 0 0 0.5]))))))

(extend-type core.context.Context
  core.context/DebugRender
  (debug-render-before-entities [ctx g]
    (tile-debug g ctx))

  (debug-render-after-entities [ctx g]
    #_(geom-test g ctx)
    (highlight-mouseover-tile g ctx)))

