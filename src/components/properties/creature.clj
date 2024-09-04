(ns components.properties.creature
  (:require [clojure.string :as str]
            [utils.core :refer [readable-number safe-merge]]
            [core.component :as component :refer [defcomponent]]
            [core.context :as ctx]))

; player doesn;t need aggro-range/reaction-time
; stats armor-pierce wrong place
; assert min body size from core.entity
(defcomponent :body/width   {:data :pos})
(defcomponent :body/height  {:data :pos})
(defcomponent :body/flying? {:data :boolean})

(defcomponent :entity/body
  {:data [:map [:body/width
                :body/height
                :body/flying?]]})

(defcomponent :creature/species
  {:data [:qualified-keyword {:namespace :species}]}
  (component/create [[_ species] _ctx]
    (str/capitalize (name species)))
  (component/info-text [[_ species] _ctx]
    (str "[LIGHT_GRAY]Creature - " species "[]")))

(defcomponent :creature/level
  {:data :pos-int}
  (component/info-text [[_ lvl] _ctx]
    (str "[GRAY]Level " lvl "[]")))

(defcomponent :properties/creatures
  (component/create [_ _ctx]
    {:schema [:entity/body
              :property/pretty-name
              :creature/species
              :creature/level
              :entity/animation
              :entity/stats
              :entity/skills
              [:entity/modifiers {:optional true}]
              [:entity/inventory {:optional true}]]
     :overview {:title "Creatures"
                :columns 15
                :image/scale 1.5
                :sort-by-fn #(vector (:creature/level %)
                                     (name (:creature/species %))
                                     (name (:property/id %)))
                :extra-info-text #(str (:creature/level %))}}))

(defn- ->body [{:keys [body/width body/height body/flying?]}]
  {:width  width
   :height height
   :collides? true
   :z-order (if flying? :z-order/flying :z-order/ground)})

(defcomponent :tx/creature
  {:let {:keys [position creature-id components]}}
  (component/do! [_ ctx]
    (let [props (ctx/property ctx creature-id)]
      [[:tx/create
        position
        (->body (:entity/body props))
        (-> props
            (dissoc :entity/body)
            (assoc :entity/destroy-audiovisual :audiovisuals/creature-die)
            (safe-merge components))]])))


; TODO spawning on player both without error ?! => not valid position checked
; also what if someone moves on the target posi ? find nearby valid cell ?

; BLOCKING PLAYER MOVEMENT ! (summons no-clip with player ?)
; check not blocked position // line of sight.
; limit max. spawns
; animation/sound
; proper icon (grayscaled ?)
; keep in player movement range priority ( follow player if too far, otherwise going for enemies)
; => so they follow you around

; not try-spawn, but check valid-params & then spawn !

; new UI -> show creature body & then place
; >> but what if it is blocked the area during action-time ?? <<

; Also: to make a complete game takes so many creatures, items, skills, balance, ui changes, testing
; is it even possible ?

(comment
 ; keys: :faction(:source)/:target-position/:creature-id
 )

; => one to one attr!?
(defcomponent :effect/spawn
  {:data [:one-to-one :properties/creatures]
   :let {:keys [property/id]}}
  (component/applicable? [_ {:keys [effect/source effect/target-position]}]
    ; TODO line of sight ? / not blocked tile..
    ; (part of target-position make)
    (and (:entity/faction @source)
         target-position))

  (component/do! [_ {:keys [effect/source effect/target-position]}]
    [[:tx/sound "sounds/bfxr_shield_consume.wav"]
     [:tx/creature {:position target-position
                    :creature-id id
                    :components #:entity {:state [:state/npc :npc-idle]
                                          :faction (:entity/faction @source)}}]]))
