(ns com.dx.playground-drawflow.viewport)

(defn viewport-transform [{:keys [x y zoom]}]
  (let [zoom-value (or zoom 1)
        x-value (or x 0)
        y-value (or y 0)]
    (str "translate(" x-value "px, " y-value "px) scale(" zoom-value ")")))

(defn viewport-style [viewport]
  {:position "absolute"
   :top 0
   :left 0
   :transform (viewport-transform viewport)
   :transform-origin "0 0"
   :z-index 2})

(defn screen->canvas
  "Convert screen coordinates to canvas coordinates"
  [screen-x screen-y viewport container-rect]
  (let [zoom (:zoom viewport 1)
        viewport-x (:x viewport 0)
        viewport-y (:y viewport 0)
        container-left (.-left container-rect)
        container-top (.-top container-rect)]
    {:x (/ (- screen-x container-left viewport-x) zoom)
     :y (/ (- screen-y container-top viewport-y) zoom)}))

(defn canvas->screen
  "Convert canvas coordinates to screen coordinates"
  [canvas-x canvas-y viewport container-rect]
  (let [zoom (:zoom viewport 1)
        viewport-x (:x viewport 0)
        viewport-y (:y viewport 0)
        container-left (.-left container-rect)
        container-top (.-top container-rect)]
    {:x (+ container-left viewport-x (* canvas-x zoom))
     :y (+ container-top viewport-y (* canvas-y zoom))}))

