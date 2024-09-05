(ns components.world.line-of-sight
  (:require [gdx.graphics.camera :as camera]
            [core.context :as ctx]))

; does not take into account zoom - but zoom is only for debug ???
; vision range?
(defn- on-screen? [entity* ctx]
  (let [[x y] (:position entity*)
        x (float x)
        y (float y)
        [cx cy] (camera/position (ctx/world-camera ctx))
        px (float cx)
        py (float cy)
        xdist (Math/abs (- x px))
        ydist (Math/abs (- y py))]
    (and
     (<= xdist (inc (/ (float (ctx/world-viewport-width ctx))  2)))
     (<= ydist (inc (/ (float (ctx/world-viewport-height ctx)) 2))))))

(def ^:private ^:dbg-flag los-checks? true)

(extend-type core.context.Context
  core.context/WorldLineOfSight
  ; does not take into account size of entity ...
  (line-of-sight? [context source* target*]
    (and (:z-order target*)  ; is even an entity which renders something TODO still necessary??
         (or (not (:entity/player? source*))
             (on-screen? target* context))
         (not (and los-checks? ; TODO at wrong point
                   (ctx/ray-blocked? context (:position source*) (:position target*)))))))
