(ns components.entity.temp-modifier
  (:require [utils.core :refer [readable-number]]
            [core.component :as component :refer [defcomponent]]
            [core.context :as ctx]
            [core.graphics :as g]))

(defcomponent :entity/temp-modifier
  {:let {:keys [counter modifiers]}}
  (component/info-text [_ ctx]
    (str "[LIGHT_GRAY]Spiderweb - remaining: " (readable-number (ctx/finished-ratio ctx counter)) "/1[]"))

  (component/tick [[k _] eid ctx]
    (when (ctx/stopped? ctx counter)
      [[:tx/dissoc eid k]
       [:tx/reverse-modifiers eid modifiers]]))

  (component/render-above [_ entity* g ctx]
    (g/draw-filled-circle g (:position entity*) 0.5 [0.5 0.5 0.5 0.4])))

(def ^:private modifiers {:modifier/movement-speed {:op/mult -0.5}})
(def ^:private duration 5)

(defcomponent :effect.entity/spiderweb
  {:data :some}
  (component/info-text [_ _effect-ctx]
    "Spiderweb slows 50% for 5 seconds."
    ; modifiers same like item/modifiers has info-text
    ; counter ?
    )

  (component/applicable? [_ {:keys [effect/source effect/target]}]
    ; ?
    true)

  ; TODO stacking? (if already has k ?) or reset counter ? (see string-effect too)
  (component/do! [_ {:keys [effect/source effect/target] :as ctx}]
    (when-not (:entity/temp-modifier @target)
      [[:tx/apply-modifiers target modifiers]
       [:tx/assoc target :entity/temp-modifier {:modifiers modifiers
                                                :counter (ctx/->counter ctx duration)}]])))
