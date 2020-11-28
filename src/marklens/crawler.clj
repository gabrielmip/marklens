(ns marklens.crawler
  (:require [pl.danieljanus.tagsoup :as clj-tagsoup]
            [clojure.string :as str]))

(defn get-children
  ([_]
    [])
  ([_ _]
    [])
  ([_ _ & children]
    children))

(defn has-tag? [node] (keyword? (first node)))

; permitir ou não tags code???
(defn allowed-tag?
  [node]
  (let [forbidden-tags #{:script :noscript :style :template :code}
        tag (first node)]
    (not (contains? forbidden-tags tag))))

(defn- texts-from-hiccup
  [parsed-page]
  (filter
    ; TODO: em vez de só considerar strings, considerar todo mundo e pegar atributos interessantes que são texto também, como o :title
    (fn [node] (string? node))
    (tree-seq
      (fn [node]
        (and
          (has-tag? node)
          (allowed-tag? node)
          (seq (apply get-children node))
          (vector? node)))
      (fn [node] (apply get-children node))
      parsed-page)))

(defn text-from-page!
  [^String url]
  (str/join
   " "
   (texts-from-hiccup (try (clj-tagsoup/parse url)
                           (catch Exception _ [])))))
