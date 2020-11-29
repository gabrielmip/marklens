(ns marklens.search-engine.tensorizer)

(defn calculate-tfidf
  [tf ndocs total-ndocs]
  (let [ln (fn [num] (/ (Math/log num)
                        (Math/log Math/E)))
        idf (+ 1 (ln (/ total-ndocs ndocs)))]
    (* tf idf)))

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
  (map
   (fn [[document-id terms-stats]]
     {:document_id document-id
      :tensor (as-tfidf terms-stats (get-empty-tensor search-term-ids))})
   (group-by :document_id doc-term-stats)))
