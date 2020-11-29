(ns marklens.search-engine.ranking)

(defn square
  [number]
  (* number number))

(defn norm
  [tensor]
  (Math/sqrt (apply + (map square tensor))))

(defn dot-product
  [tensor base]
  (apply + (map * tensor base)))

(defn calculate-similarity
  [tensor base]
  (/ (dot-product tensor base)
     (* (norm tensor) (norm base))))

(defn set-similarity
  [document query-tensor]
  (assoc document
         :similarity
         (calculate-similarity (:tensor document) query-tensor)))

(defn count-non-zeros
  [tensor]
  (reduce
    (fn [c dimension]
      (if (zero? dimension)
          c
          (+ c 1)))
    0
    tensor))

(defn rank-documents
  [documents query-tensor]
  (reverse
    (sort-by
      (fn [{similarity :similarity tensor :tensor}]
        (vector (count-non-zeros tensor) similarity))
      (map #(set-similarity % query-tensor) documents))))
