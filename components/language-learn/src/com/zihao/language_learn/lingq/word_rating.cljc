(ns com.zihao.language-learn.lingq.word-rating)

(defn button [{:keys [on variant text]}] 
  (let [common ["inline-flex" "items-center" "cursor-pointer" "justify-center" "rounded-md" "text-sm" "font-medium" "ring-offset-background" "transition-colors" "focus-visible:outline-none" "focus-visible:ring-2" "focus-visible:ring-ring" "focus-visible:ring-offset-2" "disreabled:pointer-events-none" "disabled:opacity-50" "h-10" "px-4" "py-2"
                "hover:text-accent-foreground" "border-input"]]
    [:button {:on on
              :class
              (concat
               common
               (if (= variant "default")
                 ["bg-primary" "text-primary-foreground" "hover:bg-primary/90"]
                 ["bg-background" "hover:bg-accent" "border"]))}
     text]))

(defn word-rating-ui [{:keys [word current-rating]}] 
  [:div {:class ["flex" "space-x-2"]}
   (for [rating [:again :hard :good :easy]]
     (button {:on {:click [[:data/command
                            {:command/kind :command/update-word-rating
                             :command/data {:word word
                                            :rating rating}}
                            {:on-success [[:data/query {:query/kind :query/get-word-rating}]]}]]}
              :variant (if (= current-rating rating) "default" "outline")
              :text (name rating)}))])
