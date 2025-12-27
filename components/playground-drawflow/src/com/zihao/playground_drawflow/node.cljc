(ns com.zihao.playground-drawflow.node)

;; Custom node body component example
;; This demonstrates how to create a custom node body with custom handles
(defn custom-process-node-body [node _connection render-handle-helper]
  (let [node-data (:data node)
        is-selected (boolean (:selected? node))]
    [:div
     {:style {:position "relative"
              :display "flex"
              :flex-direction "column"
              :height "100%"}}
     ;; Custom header with icon
     [:div.node-header
      {:style {:font-weight "bold"
               :margin-bottom "12px"
               :padding-bottom "8px"
               :border-bottom "2px solid #3b82f6"
               :color (if is-selected "#1e40af" "#3b82f6")
               :display "flex"
               :align-items "center"
               :gap "8px"}}
      [:span {:style {:font-size "18px"}} "⚙️"]
      (or (:label node-data) "Process Node")]
     
     ;; Custom body with multiple sections
     [:div.node-body
      {:style {:display "flex"
               :flex-direction "column"
               :gap "8px"
               :flex "1"}}
      [:div {:style {:padding "8px"
                     :background-color "#f3f4f6"
                     :border-radius "4px"}}
       "Status: " [:strong "Running"]]
      [:div {:style {:color "#6b7280"}}
       (or (:content node-data) "Custom process node")]]
     
     ;; Handles positioned using CSS
     ;; Input handle on left side
     [:div {:style {:position "absolute"
                    :left "0"
                    :top "50%"
                    :transform "translateY(-50%)"}}
      (render-handle-helper "input" :target)]
     ;; Output handles on right side
     [:div {:style {:position "absolute"
                    :right "0"
                    :top "20%"
                    :transform "translateY(-50%)"}}
      (render-handle-helper "output-top" :source)]
     [:div {:style {:position "absolute"
                    :right "0"
                    :top "80%"
                    :transform "translateY(-50%)"}}
      (render-handle-helper "output-bottom" :source)]]))

;; Another custom node body example - simple card style
(defn custom-card-node-body [node _connection render-handle-helper]
  (let [node-data (:data node)
        is-selected (boolean (:selected? node))]
    [:div
     {:style {:position "relative"
              :display "flex"
              :flex-direction "column"
              :height "100%"}}
     ;; Card-style header
     [:div.node-header
      {:style {:background-color (if is-selected "#dbeafe" "#e5e7eb")
               :padding "10px"
               :border-radius "6px 6px 0 0"
               :margin "-12px -12px 12px -12px"
               :font-weight "600"
               :color (if is-selected "#1e40af" "#374151")}}
      (or (:label node-data) "Card Node")]
     
     ;; Card body
     [:div.node-body
      {:style {:min-height "60px"
               :flex "1"}}
      [:div {:style {:font-size "14px"
                     :line-height "1.5"}}
       (or (:content node-data) "Card content here")]]
     
     ;; Handles positioned using CSS
     ;; Input handle on left side
     [:div {:style {:position "absolute"
                    :left "0"
                    :top "50%"
                    :transform "translateY(-50%)"}}
      (render-handle-helper "input" :target)]
     ;; Output handle on right side
     [:div {:style {:position "absolute"
                    :right "0"
                    :top "50%"
                    :transform "translateY(-50%)"}}
      (render-handle-helper "output" :source)]]))
