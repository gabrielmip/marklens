(ns marklens.indexer
  (:require [clojure.string :as string]
            [marklens.tokenizer :as tokenizer]
            [marklens.storage :as storage]
            [marklens.crawler :as crawler]))

(defn- group-indexes [tokens]
  (into {}
    (map
      (fn [[key appearances]]
          [key (map #(:index %) appearances)])
      (group-by #(:term %) tokens))))

(defn- tokens-for-index [text stop-words]
  (let [tokens (tokenizer/tokenize text stop-words)
        unique-tokens (distinct (map #(:term %) tokens))
        counts-by-token (frequencies (map #(:term %) tokens))
        indexes-by-token (group-indexes tokens)]
    (map
      (fn [token]
        {:term token
         :frequency (get counts-by-token token)
         :indexes (get indexes-by-token token)})
      unique-tokens)))

(defn- save-tokens! [tokens]
   (map ; o map está zipando o resultado da inserção com os tokens
    (fn [saved-id token]
        (assoc token :rowid saved-id))
    (storage/insert-terms! (map #(:term %) tokens))
    tokens))

(defn- save-page! [page tokens]
  (let [saved-page (assoc page :rowid (storage/insert-document! page))
        saved-tokens (save-tokens! tokens)]
    (storage/insert-frequencies! saved-page saved-tokens)))

(defn- filter-unseen-pages! [pages]
  (let [already-parsed (storage/get-parsed-urls!)]
    (filter
      #(not (contains? already-parsed (:url %)))
      pages)))

(defn- count-words [tokens]
  (reduce
   +
   (map
     #(count (:indexes %))
     tokens)))

(defn index-pages! [pages]
  (let [stop-words (set (string/split-lines (slurp "resources/stopwords.txt")))
        unseen-pages (filter-unseen-pages! pages)]
    (when (seq unseen-pages)
      (println (str "Unseen pages to be indexed: " (count unseen-pages))))
    (doall
     (map-indexed
      (fn [index page]
        (println (str "(" (+ index 1) "/" (count unseen-pages) ") " (:url page)))
        (let [content (crawler/text-from-page! (:url page))
              tokens (tokens-for-index content stop-words)
              page-to-save (assoc page :nwords (count-words tokens)
                                  :content content)]
          (save-page! page-to-save tokens)))
      unseen-pages))))
