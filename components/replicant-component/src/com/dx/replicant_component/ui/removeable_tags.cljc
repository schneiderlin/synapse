(ns com.dx.replicant-component.ui.removeable-tags)

(defn removable-tag
  "A removable tag component for displaying active filters
   - :child: The text to display in the tag
   - :on-remove: Handler function called when the X button is clicked"
  [{:keys [child on-remove]}]
  (let [base-classes ["badge" "gap-2" "cursor-pointer" "hover:badge-outline"]]
    [:div {:class base-classes
           :on {:click on-remove}}
     child
     [:svg {:class ["w-3" "h-3" "ml-1"]
            :fill "none"
            :stroke "currentColor"
            :viewBox "0 0 24 24"}
      [:path {:stroke-linecap "round"
              :stroke-linejoin "round"
              :stroke-width "2"
              :d "M6 18L18 6M6 6l12 12"}]]]))

(defn removable-tags-list
  "A container for multiple removable tags
   - :tags: Vector of tag data maps with :child and :on-remove keys"
  [{:keys [tags on-remove]}]
  [:div {:class ["flex" "flex-wrap" "gap-2" "items-center"]}
   (for [tag tags]
     (removable-tag {:child tag :on-remove on-remove}))])