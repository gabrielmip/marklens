(ns marklens.search-engine.searcher
  (:require [marklens.search-engine.ranking :as ranking]
            [marklens.search-engine.tensorizer :as tensorizer]
            [marklens.storage :as storage]
            [marklens.tokenizer :as tokenizer]
            [marklens.utils :as utils]))

(defn- tokens-to-terms [tokens]
  (map #(:term %) tokens))

(defn- extract-terms
  [query]
  (->
   query
   (tokenizer/tokenize #{})
   tokens-to-terms
   distinct))

(defn- merge-doc-and-term-stats
  [term-stats doc-term-stats]
  (let [term-stats-by-id (utils/index-by :term_id term-stats)]
    (map
     (fn [doc]
       (merge doc (get term-stats-by-id (:term_id doc))))
     doc-term-stats)))

(defn- get-doc-term-stats!
  [term-ids]
  (merge-doc-and-term-stats
   (storage/query-term-stats! term-ids)
   (storage/query-doc-term-stats! term-ids)))

(defn- tensorize-docs-and-query
  [query-term-ids]
  (let [doc-term-stats (get-doc-term-stats! query-term-ids)
        term-stats (storage/query-term-stats! query-term-ids)]
    {:docs (tensorizer/tensorize-documents doc-term-stats query-term-ids)
     :query (tensorizer/tensorize-query query-term-ids term-stats)}))

(defn- assoc-rank-result-in-doc
  [rank-result documents-by-id]
  (assoc (get documents-by-id (:document_id rank-result))
         :similarities (:similarities rank-result)))

(defn- get-best-n-documents-from-tensors
  [doc-tensors query-tensor n-results]
  (let [results (take n-results (ranking/rank-documents doc-tensors query-tensor))
        documents-by-id (storage/get-indexed-documents! (map #(:document_id %) results))]
    (map
     #(assoc-rank-result-in-doc % documents-by-id)
     results)))

(defn search
  ([query] (search query 10))
  ([query n-results]
   (let [query-term-ids (storage/query-term-ids! (extract-terms query))
         {doc-tensors :docs query-tensor :query} (tensorize-docs-and-query query-term-ids)]
     (get-best-n-documents-from-tensors doc-tensors query-tensor n-results))))
