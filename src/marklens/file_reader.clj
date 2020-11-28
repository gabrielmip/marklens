(ns marklens.file-reader
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [java-time :as java-time]))


(defn- bookmark-epoch-to-timestamp [epoch-str]
  (let [base (java-time/local-date-time 1601 1 1)
        epoch-as-elapsed-millis (/ (Long/parseLong epoch-str) 1000)]
    (java-time/plus base (java-time/duration epoch-as-elapsed-millis))))

(defn- gen-stringified-int []
  (let [choices (map char (range 48 58))]
    (apply
      str
      (repeatedly 10 #(rand-nth choices)))))

(def stringified-integers
  (set (repeatedly 10 gen-stringified-int)))

(def numeric-str (s/and string? #(re-matches #"\d+" %)))

(s/def ::bookmark-file (s/keys :req-un [::roots]))

(s/def ::roots (s/map-of keyword? ::node))

(s/def ::folder
  (s/and
    #(= (:type %) "folder")
    (s/keys :req-un [::date_added ::id ::name ::type ::children])))

(s/def ::page
  (s/and
    #(= (:type %) "url")
    (s/keys :req-un [::date_added ::id ::name ::type])))

(s/def ::node
  (s/and
    (s/or :folder ::folder :page ::page)
    (s/conformer #(second %))))

; o generator aqui não funciona e eu não entendo porque
; (s/def ::children
;   (s/with-gen
;     (s/coll-of ::node)
;     #(gen/set (s/gen ::page))))
(s/def ::children (s/coll-of ::node))

(s/def ::date_added
  (s/with-gen
    (s/and numeric-str (s/conformer bookmark-epoch-to-timestamp))
    #(s/gen stringified-integers)))

(s/def ::name string?)

(s/def ::type
  (s/with-gen
    (s/and string? #{"folder" "url"})
    #(s/gen #{"folder" "url"})))

(s/def ::id
  (s/with-gen numeric-str #(s/gen stringified-integers)))

(defn- file-to-tree
  "Parse file content to expected format. Dates are converted to java.time.LocalDateTime."
  [file-name]
  (s/conform ::bookmark-file (json/read-json (slurp file-name))))

(defn- append-parents-to-nodes
  [nodes parent]
  (map
    #(assoc % :parents
      (conj (:parents parent) (:name parent))) nodes))

(defn- get-pages-from-root
  [bookmark-tree-root]
  (tree-seq
    #(contains? % :children)
    #(append-parents-to-nodes (:children %) %)
    bookmark-tree-root))

(defn- remove-unwanted-keys
  [pages]
  (map #(dissoc % :children :guid :type :date_modified :id) pages))

(defn- tree-to-pages
  "Traverses tree and returns list of pages. :parents is added to pages and contains their parent folders in reverse."
  [bookmark-tree]
  (filter (fn [node] (contains? node :url))
    (flatten
      (map
        (fn [[_ root]]
          (remove-unwanted-keys
            (get-pages-from-root (assoc root :parents ()))))
        (:roots bookmark-tree)))))

; genealogy to string:
; (fn [page] #(clojure.string/join " > " (reverse (:parents page)))
(defn get-pages-from-file
  "Reads bookmark JSON file and returns a list of pages."
  [file-name]
  (tree-to-pages (file-to-tree file-name)))
