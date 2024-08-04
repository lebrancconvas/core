(ns context.ecs
  (:require [clj-commons.pretty.repl :as p]
            [utils.core :refer [sort-by-order]]
            [core.component :refer [defcomponent] :as component]
            [api.context :refer [transact-all! get-entity]]
            [api.graphics :as g]
            [api.entity :as entity :refer [map->Entity]]
            [api.tx :refer [transact!]]))

(defmethod transact! :tx/setup-entity [[_ an-atom uid components] ctx]
  {:pre [(not (contains? components :entity/id))
         (not (contains? components :entity/uid))]}
  (let [entity* (-> components
                    (component/update-map entity/create-component components ctx)
                    map->Entity)]
    (reset! an-atom (assoc entity* :entity/id an-atom :entity/uid uid)))
  nil)

(defmethod transact! :tx/assoc-uids->entities [[_ entity] {:keys [context/uids-entities] :as ctx}]
  {:pre [(number? (:entity/uid @entity))]}
  (swap! uids-entities assoc (:entity/uid @entity) entity)
  nil)

(defmethod transact! :tx/dissoc-uids->entities [[_ uid] {:keys [context/uids-entities]}]
  {:pre [(contains? @uids-entities uid)]}
  (swap! uids-entities dissoc uid)
  nil)

(defcomponent :entity/uid {}
  (entity/create [_ {:keys [entity/id]} _ctx]
    [[:tx/assoc-uids->entities id]])
  (entity/destroy [[_ uid] _entity* _ctx]
    [[:tx/dissoc-uids->entities uid]]))

(let [cnt (atom 0)]
  (defn- unique-number! []
    (swap! cnt inc)))

(defn- apply-system-transact-all! [ctx system entity*]
  (run! #(transact-all! ctx %) (component/apply-system system entity* ctx)))

(comment

 ; * entity/position obligatory
 ; * all components which use render systems depend on z-order

 [:map {:closed true}
  [:entity/animation {:optional true}] ; :entity/image shouldn't be there because we assoc it
  [:entity/body {:optional true}]
  [:entity/clickable {:optional true}] ; depends on z-order (mouseover-entity filters) & body ( world-grid )
  [:entity/delete-after-animation-stopped? ] ; -> animation ofco.
  [:entity/delete-after-duration {:optional true}] ; no deps.
  [:entity/faction {:optional true}] ; TODO why projectiles have this ?!
  ; => context.potential-field/tiles->entities
  ; => projectile collision don't collide with same faction ...
  ; so projectile _needs_ it because projectile-collision depends on it ....
  ; dann noch 'shout' und die API fns friendly/enemy....
  ; might fix performance if all projectiles don't cause each a potential field ,....
  [:entity/flying?]
  ; no -> used @ world.cell/cell-blocked? ....
  [:entity/hp] ; => body for render....
  [:entity/image] ; position, if body -> reotation angle ?!,
  ; all who use entity/render need z-order too !!
  ; I wonder if z-order belongs in body .....
  ; and body checks that collides? is off for z-order effect/debug/on-ground ?
  [:entity/inventory] ; TODO modifier ... that means if inventory need _all_ modifiable ethingyies or have to check if component is there ...
  [:entity/line-render] ; no deps
  [:entity/mana] ; no deps
  [:entity/mouseover] ; depends on :entity/body
  [:entity/movement] ; depends on :entity/body
  [:entity/plop] ; TODO fix - destroy just for removing data, not making game side effects ( ?!)
  [:entity/position] ; required - because adds/removes from world with create/destroy
  [:entity/projectile-collision] ; depends on :entity/body, :entity/faction (no friendly fire)
  [:entity/reaction-time]
  [:entity/shout] ; depends on :entity/faction
  [:entity/skills] ; to be usable @ entity/state it depends on :entity/mana - specific skills also require specific components ?! e.g. strength for melee - attack ....

  [:entity/state]
  ; doesn;t depend on anything but the _creature_ states depend on something! can code defensively or
  ; adjust creature schema ...
  ; TODO dependencies as of entity-state components -> move them also out of entity/ folder
  ; body, skills, mana, stats (cast,attack-speed), faction, movement (if should move ....)

  ; npc state -> reaction-time

  ; player state - :entity/click-distance-tiles, :entity/free-skill-points, inventory, item-on-cursor
  ; => player component make

  [:entity/stats] ; what do effects do if armor-save is not there ??? or has to be there ???
  [:entity/string-effect] ; depends one entity/body
  [:entity/z-order]

  ; TODO components without namespace add ... see txt

  ]

 ; dependencies just (assert k) @ the create of that component ?!
 ; or put @ component metadata ... then can check @ creation before calling create-component - fail fast...
 )

(defmethod transact! :tx/create [[_ components] ctx]
  (let [entity (atom nil)]
    (transact-all! ctx [[:tx/setup-entity entity (unique-number!) components]])
    (apply-system-transact-all! ctx entity/create @entity))
  [])

(defmethod transact! :tx/destroy [[_ entity] _ctx]
  (swap! entity assoc :entity/destroyed? true)
  nil)

(defn- handle-entity-error! [{:keys [context/thrown-error]} entity* throwable]
  (p/pretty-pst (ex-info "" (select-keys entity* [:entity/uid]) throwable))
  (reset! thrown-error throwable))

(defn- render-entity* [system
                       entity*
                       g
                       {:keys [context/thrown-error] :as ctx}]
  (try
   (dorun (component/apply-system system entity* g ctx))
   (catch Throwable t
     (when-not @thrown-error
       (handle-entity-error! ctx entity* t))
     (let [[x y] (:entity/position entity*)]
       (g/draw-text g
                    {:text (str "Error / entity uid: " (:entity/uid entity*))
                     :x x
                     :y y
                     :up? true})))))

; TODO similar with define-order --- same fns for z-order keyword ... same name make ?
(def ^:private render-systems [entity/render-below
                               entity/render-default
                               entity/render-above
                               entity/render-info])

(extend-type api.context.Context
  api.context/EntityComponentSystem
  (all-entities [{:keys [context/uids-entities]}]
    (vals @uids-entities))

  (get-entity [{:keys [context/uids-entities]} uid]
    (get @uids-entities uid))

  (tick-entities! [{:keys [context/thrown-error] :as ctx} entities*]
    (doseq [entity* entities*]
      (try
       (apply-system-transact-all! ctx entity/tick entity*)
       (catch Throwable t
         (handle-entity-error! ctx entity* t)))))

  (render-entities! [context g entities*]
    (doseq [entities* (map second ; FIXME lazy seq
                           (sort-by-order (group-by :entity/z-order entities*)
                                          first
                                          entity/render-order))
            system render-systems
            entity* entities*]
      (render-entity* system entity* g context))
    (doseq [entity* entities*]
      (render-entity* entity/render-debug entity* g context)))

  (remove-destroyed-entities! [{:keys [context/uids-entities] :as ctx}]
    (doseq [entity (filter (comp :entity/destroyed? deref) (vals @uids-entities))]
      (apply-system-transact-all! ctx entity/destroy @entity))))
