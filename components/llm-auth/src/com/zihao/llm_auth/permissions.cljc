(ns com.zihao.llm-auth.permissions)

(def roles
  "Role definitions for the LLM evaluation system"
  #{:admin :agent})

(def role-permissions
  "Permissions map for each role"
  {:admin #{:dashboard/view
            :dataset/view
            :dataset/create
            :dataset/edit
            :dataset/delete
            :workflow/view
            :workflow/create
            :workflow/edit
            :workflow/delete
            :settings/view
            :users/manage}
   :agent #{:dashboard/view
            :dataset/view
            :dataset/create
            :dataset/edit
            :workflow/view
            :workflow/create}})

(def navigation-items
  "Navigation items with required permissions"
  [{:id :dashboard
    :label "Dashboard"
    :route :llm-eval/dashboard
    :required-permission :dashboard/view
    :icon "chart-bar"}
   {:id :dataset
    :label "Dataset"
    :route :llm-eval/dataset
    :required-permission :dataset/view
    :icon "table"}
   {:id :dataset-detail
    :label "Evaluation Detail"
    :route :llm-eval/dataset-detail
    :required-permission :dataset/view
    :icon "document"}
   {:id :workflow
    :label "Workflow"
    :route :llm-eval/workflow
    :required-permission :workflow/view
    :icon "flow"}])

(defn has-permission?
  "Check if a role has a specific permission"
  [role permission]
  (contains? (get role-permissions role #{}) permission))

(defn get-navigation-for-role
  "Get navigation items accessible to a role"
  [role]
  (filter (fn [item]
            (has-permission? role (:required-permission item)))
          navigation-items))

(defn check-role
  "Validate that a role is valid"
  [role]
  (contains? roles role))

(defn get-permissions-for-role
  "Get all permissions for a role"
  [role]
  (get role-permissions role #{}))

(comment
  (has-permission? :admin :users/manage)
  (has-permission? :agent :users/manage)
  (get-navigation-for-role :admin)
  (get-navigation-for-role :agent)
  (check-role :admin)
  (check-role :invalid)
  :rcf)
