(ns com.dx.replicant-component.ui.button)

(defn button [label on-click]
  [:button {:class ["mb-4" "px-4" "py-2" "bg-blue-500" "text-white" "rounded" "hover:bg-blue-600" "focus:outline-none" "focus:ring-2" "focus:ring-blue-500" "focus:ring-opacity-50"]
            :on {:click on-click}}
   label])