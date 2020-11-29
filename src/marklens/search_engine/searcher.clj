(ns marklens.search-engine.searcher
  (:require [marklens.storage :as storage]
            [marklens.search-engine.tensorizer :as tensorizer]
            [marklens.search-engine.ranking :as ranking]
            [marklens.tokenizer :as tokenizer]
            [marklens.utils :as utils]))

(defn tokens-to-terms [tokens]
  (map #(:term %) tokens))

(defn extract-terms
  [query]
  (->
    query
    (tokenizer/tokenize #{})
    tokens-to-terms
    distinct))

(defn merge-doc-and-term-stats
  [term-stats doc-term-stats]
  (let [term-stats-by-id (utils/index-by :term_id term-stats)]
    (map
      (fn [doc]
        (merge doc (get term-stats-by-id (:term_id doc))))
      doc-term-stats)))

(defn get-doc-term-stats
  [term-ids]
  (merge-doc-and-term-stats
    (storage/query-term-stats! term-ids)
    (storage/query-doc-term-stats! term-ids)))

(defn search
  ([query] (search query 10))
  ([query n-results]
   (let [terms (extract-terms query)
         term-ids (storage/query-term-ids! terms)
         doc-term-stats (get-doc-term-stats term-ids)
         term-stats (storage/query-term-stats! term-ids)
         tensorized-docs (tensorizer/tensorize-documents doc-term-stats term-ids)
         query-tensor (tensorizer/tensorize-query term-ids term-stats)
         results (take n-results (ranking/rank-documents tensorized-docs query-tensor))
         documents (storage/get-indexed-documents! (map #(:document_id %) results))]
     (map
      #(assoc (get documents (:document_id %))
              :tensor (:tensor %)
              :similarity (:similarity %))
      results))))
