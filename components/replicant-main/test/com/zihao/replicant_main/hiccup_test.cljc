(ns com.zihao.replicant-main.hiccup-test
  (:require
   [clojure.test :as t :refer [deftest is testing]]
   [malli.core :as m]
   [com.zihao.replicant-main.hiccup-test-helper :refer [contains-element?
                                                        contains-element-with-attr?
                                                        contains-element-with-content?
                                                        contains-hiccup?]]))

;; A simple Malli schema for validating hiccup-like data structures.
;; This schema accepts:
;; - :keyword (a bare tag)
;; - [:tag] (just a tag)
;; - [:tag content] (tag with content)
;; - [:tag {:attr "value"}] (tag with attributes)
;; - [:tag {:attr "value"} content] (tag with attributes and content)
;; - [:tag child1 child2 ...] (tag with multiple children)
;; - [:tag {:attr "value"} child1 child2 ...] (tag with attributes and multiple children)
(def HiccupElement
  [:or :keyword
   [:cat :keyword]
   [:cat :keyword [:* :any]]
   [:cat :keyword :map [:* :any]]])

(comment
  (t/run-test hiccup-test)
  :rcf)

(deftest hiccup-test
  (testing "valid hiccup elements"
    (is (m/validate HiccupElement [:div]))
    (is (m/validate HiccupElement [:div "hello world"]))
    (is (m/validate HiccupElement [:div {:class "my-class"}]))
    (is (m/validate HiccupElement [:div {:id "app" :class "container"} "content"]))
    (is (m/validate HiccupElement [:div [:span "nested"]]))
    (is (m/validate HiccupElement [:div [:p "text1"] "text2"]))
    (is (m/validate HiccupElement [:div [:p "text1"] [:p "text2"] [:p "text3"]])))

  (testing "bare keyword is valid hiccup"
    (is (m/validate HiccupElement :div))
    (is (m/validate HiccupElement :span)))

  (testing "invalid hiccup elements"
    (is (false? (m/validate HiccupElement ["not-a-keyword"])))
    (is (false? (m/validate HiccupElement nil)))
    (is (false? (m/validate HiccupElement 123)))
    (is (false? (m/validate HiccupElement {})))
    (is (false? (m/validate HiccupElement []))))

  (testing "contains-element? finds elements at any nesting level"
    (is (contains-element? [:div [:button "Click"]] :button))
    (is (contains-element? [:div [:div [:button "Click"]]] :button))
    (is (contains-element? [:div [:div [:div [:button "Click"]]]] :button))
    (is (not (contains-element? [:div [:span "Click"]] :button)))
    ;; list-wrapped children (e.g. from map over buttons) are still traversed
    (is (contains-element? [:div {:class ["flex" "space-x-2"]}
                           ([:button "again"] [:button "hard"])]
                         :button)))

  (testing "contains-element-with-attr? finds elements with specific attributes"
    (is (contains-element-with-attr? [:div [:button {:id "submit"} "Click"]] {:id "submit"}))
    (is (contains-element-with-attr? [:div [:div [:button {:id "submit"} "Click"]]] {:id "submit"}))
    (is (not (contains-element-with-attr? [:div [:button {:id "cancel"} "Click"]] {:id "submit"}))))

  (testing "contains-element-with-content? finds elements with specific content"
    (is (contains-element-with-content? [:div [:button "Click me"]] "Click me"))
    (is (contains-element-with-content? [:div [:div [:button "Click me"]]] "Click me"))
    (is (not (contains-element-with-content? [:div [:button "Click"]] "Click me")))
    ;; partial match (optional :partial true)
    (is (contains-element-with-content? [:div [:button "Click me"]] "Click" :partial true))
    ;; case-insensitive (optional :case-insensitive true)
    (is (contains-element-with-content? [:div [:button "Click Me"]] "click me" :case-insensitive true)))

  (testing "contains-hiccup? with keyword finds elements by tag"
    (is (contains-hiccup? [:div [:button "Click"]] :button))
    (is (contains-hiccup? [:div [:div [:button "Click"]]] :button))
    (is (not (contains-hiccup? [:div [:span "Click"]] :button))))

  (testing "contains-hiccup? with predicate finds matching elements"
    (is (contains-hiccup? [:div [:button "Click"]] #(and (vector? %) (= :button (first %)))))
    (is (contains-hiccup? [:div [:div [:button {:id "submit"} "Click"]]]
                          #(and (vector? %)
                                (= :button (first %))
                                (-> % second :id (= "submit"))))))

  (testing "complex nested structure search"
    (let [complex-hiccup
          [:div {:class "container"}
           [:header
            [:h1 "Title"]
            [:nav
             [:a {:href "/home"} "Home"]
             [:a {:href "/about"} "About"]]]
           [:main
            [:section
             [:h2 "Content"]
             [:p "Some text"]]
            [:aside
             [:button {:class "btn-primary"} "Action"]]]]]

      (is (contains-element? complex-hiccup :button))
      (is (contains-element? complex-hiccup :nav))
      (is (contains-element? complex-hiccup :h1))
      (is (contains-element-with-attr? complex-hiccup {:href "/home"}))
      (is (contains-element-with-attr? complex-hiccup {:class "btn-primary"}))
      (is (contains-element-with-content? complex-hiccup "Title"))
      (is (contains-element-with-content? complex-hiccup "Some text"))
      (is (contains-hiccup? complex-hiccup :button))
      (is (contains-hiccup? complex-hiccup :nav)))))

