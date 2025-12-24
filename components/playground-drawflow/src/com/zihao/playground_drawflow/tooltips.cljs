(ns com.zihao.playground-drawflow.tooltips)

(def tooltip-offset-y 12)
(def tooltip-offset-x 15)

(defn default-tooltip-component
  "Default tooltip component that displays handle information.
   
   Args:
   - node: The node data
   - handle-id: String identifier for the handle
   - handle-type: :source or :target
   - handle-data: Map of handle-specific data from :data :handle-data
   "
  [_node handle-id handle-type handle-data]
  [:div.tooltip-content.px-4.py-3.text-sm
     [:div.tooltip-header.font-semibold.mb-2.pb-2.border-b.border-base-300.text-base-content.flex.items-center.gap-2
      [:span "Handle:"]
      [:span.badge.badge-primary.badge-sm.font-mono handle-id]]
     [:div.tooltip-body.space-y-2.text-xs.mt-2
      [:div.flex.items-center.gap-2
       [:span {:class ["font-medium" "text-base-content/70"]} "Type:"]
       [:span.badge.badge-sm.badge-outline.badge-info (name handle-type)]]
      (when-let [value (:direct-value handle-data)]
        [:div.flex.flex-col.gap-1
         [:span {:class ["font-medium" "text-base-content/70"]} "Value:"]
         [:span.font-mono.text-xs.bg-base-200.px-2.py-1.rounded.border.border-base-300.text-base-content (pr-str value)]])
      (when-let [connection (:connection handle-data)]
        [:div.flex.flex-col.gap-1
         [:span {:class ["font-medium" "text-base-content/70"]} "Connected:"]
         [:span.badge.badge-sm.badge-success.badge-outline
          (str (:source-node-id connection) ":" (:source-handle-id connection))]])]])

(defn render-tooltip
  "Render tooltip content for a handle.
   Returns hiccup with tooltip content, no positioning.
   The handle is responsible for positioning the tooltip relative to itself.
   
   Args:
   - tooltip-state: Map with :node-id, :handle-id, :handle-type
   - node: The node that owns this tooltip
   
   Returns: Hiccup for tooltip content (no position wrapper)
   "
  [tooltip-state node]
  (when (and tooltip-state node)
    (let [{:keys [node-id handle-id handle-type]} tooltip-state
          node-data (:data node)
          handle-data-map (get node-data :handle-data {})
          handle-data (get handle-data-map handle-id {})
          tooltip-component (or (:tooltip-component handle-data)
                               default-tooltip-component)]
      [:div.tooltip-wrapper.bg-base-100.text-base-content.rounded-lg.shadow-2xl.border.border-base-300.overflow-hidden
       {:id (str "tooltip-" node-id "-" handle-id)
        :class ["tooltip" "pointer-events-auto" "opacity-0"]
        :style {:max-width "240px"
                :animation "tooltip-fade-in 0.2s ease-in forwards"}
        :on {:mouseenter [[:canvas/tooltip-mouse-enter]]
             :mouseleave [[:canvas/tooltip-mouse-leave]]}}
       (tooltip-component node handle-id handle-type handle-data)
       [:div.tooltip-arrow.absolute.w-0.h-0
        {:style {:top (str (- tooltip-offset-y) "px")
                 :left "50%"
                 :transform "translateX(-50%)"
                 :border-left "9px solid transparent"
                 :border-right "9px solid transparent"
                 :border-bottom "9px solid hsl(var(--b1))"
                 :filter "drop-shadow(0 -2px 4px rgba(0, 0, 0, 0.1))"}}]])))

