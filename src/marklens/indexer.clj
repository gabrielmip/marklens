(ns marklens.indexer
  (:require [clojure.string :as string]
            [marklens.crawler :as crawler]
            [marklens.storage :as storage]
            [marklens.tokenizer :as tokenizer]))

(defn- group-indexes [tokens]
  (into {}
        (map
         (fn [[key appearances]]
           [key (map #(:index %) appearances)])
         (group-by #(:term %) tokens))))

(defn- tokens-for-index [text stop-words origin]
  (let [tokens (tokenizer/tokenize text stop-words)
        unique-tokens (distinct (map #(:term %) tokens))
        counts-by-token (frequencies (map #(:term %) tokens))
        indexes-by-token (group-indexes tokens)]
    (map
     (fn [token]
       {:term token
        :frequency (get counts-by-token token)
        :indexes (get indexes-by-token token)
        :origin origin})
     unique-tokens)))

(defn- save-tokens! [con tokens]
  (map ; o map está zipando o resultado da inserção com os tokens
       (fn [saved-id token]
         (assoc token :rowid saved-id))
       (storage/insert-terms! con (map #(:term %) tokens))
       tokens))

(defn save-page! [con page tokens]
  (let [saved-page (assoc page :rowid (storage/insert-document! con page))
        saved-tokens (save-tokens! con tokens)]
    (println (str "saving " (:url page)))
    (storage/insert-frequencies! con saved-page saved-tokens)))

(defn filter-unseen-pages! [pages]
  (let [already-parsed (storage/get-parsed-urls!)]
    (filter
     #(not (contains? already-parsed (:url %)))
     pages)))

(defn filter-unseen-pages [pages already-parsed-urls]
  (filter
   #(not (contains? already-parsed-urls (:url %)))
   pages))

(defn- count-words [tokens]
  (reduce
   +
   (map
    #(count (:indexes %))
    tokens)))

(defn get-tokens-from-page
  [page content stop-words]
  (flatten
   (list
    (tokens-for-index content stop-words "body")
    (tokens-for-index (:url page) stop-words "url")
    (tokens-for-index (:name page) stop-words "name"))))

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
              tokens (get-tokens-from-page page content stop-words)
              page-to-save (assoc page :nwords (count-words tokens)
                                  :content content)]
          (save-page! page-to-save tokens)))
      unseen-pages))))

;[stop-words (set (string/split-lines (slurp "resources/stopwords.txt")))
;       unseen-pages (filter-unseen-pages! pages)
;       crawl crawler/text-from-page!]
; (when (seq pages)
;   (println (str "Unseen pages to be indexed: " (count pages))))
;(save-page! page-to-save tokens)
(defn pages-to-tokens
  [pages url-crawler stop-words]
  (map-indexed
   (fn [idx page]
     (println (str "(" (+ idx 1) "/" (count pages) ") " (:url page)))
     (let [content (url-crawler (:url page))
           tokens (get-tokens-from-page page content stop-words)]
       {:tokens tokens
        :page (assoc page
                     :nwords (count-words tokens)
                     :content content)}))
   pages))
