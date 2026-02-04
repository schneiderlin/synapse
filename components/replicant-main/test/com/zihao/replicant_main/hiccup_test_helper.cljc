(ns com.zihao.replicant-main.hiccup-test-helper
  "Helper functions for testing hiccup structures."
  (:require [clojure.test :refer [is]]
            [clojure.string :as string]))

;;; Assertion macros with user-friendly failure messages

(defmacro is-in-hiccup
  "Asserts hiccup contains the given content (string) or element tag (keyword).
   Use for content: (is-in-hiccup result \"译文:\")
   Use for tag: (is-in-hiccup result :textarea)
   On failure: \"<needle> is not in hiccup\" (with suggestion if partial/case-insensitive would match)."
  [hiccup needle]
  (if (keyword? needle)
    `(is (contains-element? ~hiccup ~needle)
         ~(str (pr-str needle) " is not in hiccup"))
    `(is (contains-element-with-content? ~hiccup ~needle)
         (str (pr-str ~needle) " is not in hiccup" (content-match-suggestion ~hiccup ~needle)))))

(defmacro is-not-in-hiccup
  "Asserts hiccup does not contain the given content (string) or element tag (keyword).
   On failure: \"<needle> should not be in hiccup\" (with suggestion if partial/case-insensitive would match)."
  [hiccup needle]
  (if (keyword? needle)
    `(is (not (contains-element? ~hiccup ~needle))
         ~(str (pr-str needle) " should not be in hiccup"))
    `(is (not (contains-element-with-content? ~hiccup ~needle))
         (str (pr-str ~needle) " should not be in hiccup" (content-match-suggestion ~hiccup ~needle)))))

;; Walk a hiccup structure and return true if any element matches the predicate.
;; Uses coll? so we descend into both vectors and lists (e.g. list-wrapped children
;; like ([:button "a"] [:button "b"]) are still traversed).
(defn walk-hiccup
  [hiccup pred]
  (cond
    ;; If the element itself matches, return true
    (pred hiccup) true

    ;; If it's a collection (vector or list), walk its children
    (coll? hiccup)
    (boolean (some #(walk-hiccup % pred) hiccup))

    ;; Otherwise, it's a leaf node, no match
    :else false))

;; Check if hiccup contains an element with a specific tag
(defn contains-element?
  "Check if hiccup contains an element with the given tag at any nesting level."
  [hiccup tag]
  (walk-hiccup hiccup #(and (vector? %)
                            (keyword? (first %))
                            (= (first %) tag))))

;; Check if hiccup contains an element with specific attributes
(defn contains-element-with-attr?
  "Check if hiccup contains an element with the given attributes at any nesting level.
   attrs can be a map or a predicate function."
  [hiccup attrs]
  (let [attr-pred (if (map? attrs)
                    #(and (map? %) (every? (fn [[k v]] (= (get % k) v)) attrs))
                    attrs)]
    (walk-hiccup hiccup #(and (vector? %)
                              (> (count %) 1)
                              (map? (second %))
                              (attr-pred (second %))))))

;; Check if hiccup contains an element with specific content
(defn contains-element-with-content?
  "Check if hiccup contains an element containing the given content at any nesting level.
   content can be a value or a predicate function.

   Optional keyword args:
   :partial - when true, match when content appears as substring in a string node
   :case-insensitive - when true, match strings ignoring case

   When neither option is given and exact match fails, is-in-hiccup/is-not-in-hiccup
   will suggest trying :partial or :case-insensitive if either would match."
  [hiccup content & opts]
  (let [{:keys [partial case-insensitive]} (apply hash-map opts)
        content-pred (cond
                       (fn? content) content
                       (and partial case-insensitive)
                       #(and (string? %) (string/includes? (string/lower-case %) (string/lower-case (str content))))
                       partial
                       #(and (string? %) (string/includes? (str %) (str content)))
                       case-insensitive
                       #(and (string? %) (= (string/lower-case (str %)) (string/lower-case (str content))))
                       :else
                       #(= % content))]
    (walk-hiccup hiccup content-pred)))

(defn content-match-suggestion
  "When exact content match fails, check :partial and :case-insensitive.
   Returns a string to append to the failure message, or empty string.
   Only applies when content is not a predicate (fn?)."
  [hiccup content]
  (if (fn? content)
    ""
    (let [exact (contains-element-with-content? hiccup content)
          partial-match (contains-element-with-content? hiccup content :partial true)
          case-insensitive-match (contains-element-with-content? hiccup content :case-insensitive true)]
      (if (and (not exact) (or partial-match case-insensitive-match))
        (str " (maybe try "
             (cond (and partial-match case-insensitive-match) ":partial true or :case-insensitive true?"
                   partial-match ":partial true?"
                   :else ":case-insensitive true?")
             ")")
        ""))))

;; Check if hiccup contains a specific pattern
(defn contains-hiccup?
  "Check if hiccup contains an element matching the given pattern at any nesting level.
   Pattern can be:
   - A keyword tag: check for element with that tag
   - A predicate function: check for element matching the predicate
   - A map of attributes: check for element with those attributes"
  [hiccup pattern]
  (cond
    (keyword? pattern) (contains-element? hiccup pattern)
    (fn? pattern) (walk-hiccup hiccup pattern)
    (map? pattern) (contains-element-with-attr? hiccup pattern)
    :else (throw (ex-info "Invalid pattern" {:pattern pattern}))))
