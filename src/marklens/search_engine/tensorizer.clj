(ns marklens.search-engine.tensorizer
  (:require [marklens.utils :as utils]))

(defn- unsafe-calculate-tfidf
  [tf ndocs total-ndocs]
  (let [ln (fn [num] (/ (Math/log num)
                        (Math/log Math/E)))
        idf (+ 1 (ln (/ total-ndocs ndocs)))]
    (* tf idf)))

(defn calculate-tfidf
  [tf ndocs total-ndocs]
  (if (every? integer? [tf ndocs total-ndocs])
      (unsafe-calculate-tfidf tf ndocs total-ndocs)
      0))

(defn get-empty-tensor
  [search-term-ids]
  (into {} (map #(vector % 0) search-term-ids)))

(defn as-tfidf
  [terms-stats empty-tensor]
  (vals
   (reduce
    (fn [tensor
         {term-id :term_id
          frequency :frequency
          ndocs :ndocs
          total-ndocs :total_ndocs}]
      (assoc tensor term-id
             (calculate-tfidf frequency ndocs total-ndocs)))
    empty-tensor
    terms-stats)))

(defn tensorize-documents
  [doc-term-stats search-term-ids]
  (vals
   (reduce
    (fn [by-documents [group terms-stats]]
      (assoc by-documents
             (:document_id group) (assoc (get by-documents (:document_id group))
                                         :document_id (:document_id group)
                                         (keyword (:origin group)) (as-tfidf terms-stats (get-empty-tensor search-term-ids)))))
    {}
    (group-by #(select-keys % [:document_id :origin]) doc-term-stats))))

(defn query-term-as-dimension
  [term-id stats]
  {:term_id term-id
    :frequency 1
    :ndocs (:ndocs stats)
    :total_ndocs (:total_ndocs stats)})

(defn tensorize-query
  [term-ids term-stats]
  (let [indexed-stats (utils/index-by :term_id term-stats)]
    (as-tfidf
     (map
      #(query-term-as-dimension % (get indexed-stats %))
      term-ids)
     (get-empty-tensor term-ids))))
