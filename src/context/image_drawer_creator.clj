(ns context.image-drawer-creator
  (:require [api.context :as ctx])
  (:import com.badlogic.gdx.graphics.Texture
           com.badlogic.gdx.graphics.g2d.TextureRegion))

(defn- texture-region-dimensions [^TextureRegion texture-region]
  [(.getRegionWidth  texture-region)
   (.getRegionHeight texture-region)])

(defn- scale-dimensions [dimensions scale]
  (mapv (comp float (partial * scale)) dimensions))

(defn- assoc-dimensions [{:keys [texture-region scale] :as image} world-unit-scale]
  {:pre [(number? world-unit-scale)
         (or (number? scale)
             (and (vector? scale)
                  (number? (scale 0))
                  (number? (scale 1))))]}
  (let [pixel-dimensions (if (number? scale)
                           (scale-dimensions (texture-region-dimensions texture-region) scale)
                           scale)]
    (assoc image
           :pixel-dimensions pixel-dimensions
           :world-unit-dimensions (scale-dimensions pixel-dimensions world-unit-scale))))

; (.getTextureData (.getTexture (:texture-region (first (:frames (:animation @(game.db/get-entity 1)))))))
; can remove :file @ Image because its in texture-data
; only TextureRegion doesn't have toString , can implement myself ? so can see which image is being used (in case)
(defrecord Image [file
                  texture-region
                  sub-image-bounds ; => is in texture-region data?
                  scale
                  pixel-dimensions
                  world-unit-dimensions
                  tilew
                  tileh])
; color missing ? - used @ drawer ....

(defn- ->texture-region [ctx file & [x y w h]]
  (let [^Texture texture (ctx/cached-texture ctx file)]
    (if (and x y w h)
      (TextureRegion. texture (int x) (int y) (int w) (int h))
      (TextureRegion. texture))))

; TODO pass texture-region ....

(extend-type api.context.Context
  api.context/ImageCreator
  (create-image [{{:keys [world-unit-scale]} :context/graphics :as ctx} file]
    (assoc-dimensions (map->Image {:file file
                                   :scale 1 ; not used anymore as arg (or scale 1) because varargs protocol methods not possible, anyway refactor images
                                   ; take only texture-region, scale,color
                                   :texture-region (->texture-region ctx file)})
                      world-unit-scale))

  (get-scaled-copy [{{:keys [world-unit-scale]} :context/graphics} image scale]
    (assoc-dimensions (assoc image :scale scale)
                      world-unit-scale))

  (get-sub-image [{{:keys [world-unit-scale]} :context/graphics :as ctx}
                  {:keys [file sub-image-bounds] :as image}]
    (assoc-dimensions (assoc image
                             :scale 1
                             :texture-region (apply ->texture-region ctx file sub-image-bounds)
                             :sub-image-bounds sub-image-bounds)
                      world-unit-scale))

  (spritesheet [context file tilew tileh]
    (assoc (api.context/create-image context file) :tilew tilew :tileh tileh))

  (get-sprite [context {:keys [tilew tileh] :as sheet} [x y]]
    (api.context/get-sub-image context
                                 (assoc sheet :sub-image-bounds [(* x tilew) (* y tileh) tilew tileh]))))
