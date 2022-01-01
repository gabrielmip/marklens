(ns marklens.etl.chrome.file-reader
  (:require [clojure.data.json :as json]
            [clojure.spec.alpha :as s]
            [clojure.spec.gen.alpha :as gen]
            [java-time :as java-time]))

(comment
  "how to sample a bookmark file content"
  (require '[marklens.tree-spec :refer [gen-overrides-for-max-depth]])
  (gen/sample
   (s/gen ::bookmark-file
          (gen-overrides-for-max-depth ::folder 3))))

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

(defn create-type-spec
  [name]
  (s/spec #(= % name)
          :gen #(s/gen #{name})))

(s/def ::bookmark-file (s/keys :req-un [::roots]))

(s/def ::roots (s/map-of keyword? ::node))

(s/def ::node
  (s/and
   (s/or :folder ::folder :page ::page)
   (s/conformer #(second %))))

(s/def ::folder
  (s/keys :req-un [::date_added ::id ::name :folder/type ::children]))

(s/def ::page
  (s/keys :req-un [::date_added ::id ::name :page/type]))

(s/def ::children (s/spec (s/coll-of ::node :gen-max 5)))

(s/def ::date_added
  (s/with-gen
    (s/and numeric-str (s/conformer bookmark-epoch-to-timestamp))
    #(s/gen stringified-integers)))

(s/def ::name string?)

(s/def :folder/type (create-type-spec "folder"))

(s/def :page/type (create-type-spec "url"))

(s/def ::type
  (s/spec (s/or :folder :folder/type
                :page :page/type)
          :gen #(gen/one-of [(s/gen :folder/type)
                             (s/gen :page/type)])))

(s/def ::id
  (s/with-gen numeric-str #(s/gen stringified-integers)))

(defn- file-to-tree
  "Parse file content to expected format. Dates are converted to java.time.LocalDateTime."
  [file-name]
  (s/conform ::bookmark-file (json/read-json (slurp file-name))))

(defn- file-to-tree-2
  "Parse file content to expected format. Dates are converted to java.time.LocalDateTime."
  [file-content]
  (s/conform ::bookmark-file (json/read-json file-content)))

; (def as-json (json/read-json (slurp "/home/gabriel/.config/BraveSoftware/Brave-Browser/Default/Bookmarks")))
; (s/explain ::bookmark-file as-json)

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

(defn get-pages-from-file-2
  "Reads bookmark JSON file and returns a list of pages."
  [file-content]
  (tree-to-pages (file-to-tree-2 file-content)))
