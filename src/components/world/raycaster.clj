(ns components.world.raycaster
  (:require [math.raycaster :as raycaster]
            [math.vector :as v]
            [data.grid2d :as grid2d]
            [core.component :as component :refer [defcomponent]]
            [core.context :as ctx]))

(defprotocol RayCaster
  (ray-blocked? [_ start target]))

(defrecord RayCasterArray [arr width height]
  RayCaster
  (ray-blocked? [_ start target]
    (raycaster/ray-blocked? arr width height start target)))

(defn- set-arr [arr cell* cell*->blocked?]
  (let [[x y] (:position cell*)]
    (aset arr x y (boolean (cell*->blocked? cell*)))))

(defcomponent :world/raycaster
  (component/create [[_ position->blocked?] {:keys [world/grid]}]
    (let [width (grid2d/width grid)
          height (grid2d/height grid)
          arr (make-array Boolean/TYPE width height)]
      (doseq [cell (grid2d/cells grid)]
        (set-arr arr @cell position->blocked?))
      (map->RayCasterArray {:arr arr
                            :width width
                            :height height}))))

; TO math.... // not tested
(defn- create-double-ray-endpositions
  "path-w in tiles."
  [[start-x start-y] [target-x target-y] path-w]
  {:pre [(< path-w 0.98)]} ; wieso 0.98??
  (let [path-w (+ path-w 0.02) ;etwas gr�sser damit z.b. projektil nicht an ecken anst�sst
        v (v/direction [start-x start-y]
                       [target-y target-y])
        [normal1 normal2] (v/get-normal-vectors v)
        normal1 (v/scale normal1 (/ path-w 2))
        normal2 (v/scale normal2 (/ path-w 2))
        start1  (v/add [start-x  start-y]  normal1)
        start2  (v/add [start-x  start-y]  normal2)
        target1 (v/add [target-x target-y] normal1)
        target2 (v/add [target-x target-y] normal2)]
    [start1,target1,start2,target2]))

(extend-type core.context.Context
  core.context/WorldRaycaster
  (ray-blocked? [{:keys [world/raycaster]} start target]
    (ray-blocked? raycaster start target))

  (path-blocked? [{:keys [world/raycaster]} start target path-w]
    (let [[start1,target1,start2,target2] (create-double-ray-endpositions start target path-w)]
      (or
       (ray-blocked? raycaster start1 target1)
       (ray-blocked? raycaster start2 target2)))))
