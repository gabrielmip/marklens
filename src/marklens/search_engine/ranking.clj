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
  (let [denominator (* (norm tensor) (norm base))]
    (if (zero? denominator)
        0
        (/ (dot-product tensor base)
           denominator))))

(defn get-tensor
  [document origin dimension-count]
  (if (contains? document origin)
      (origin document)
      (repeat dimension-count 0)))

(defn set-similarities
  [document query-tensor]
  (let [calculator (fn [origin]
                     (calculate-similarity
                      (get-tensor document origin (count query-tensor))
                      query-tensor))]
    (assoc document :similarities {:body (calculator :body)
                                   :url (calculator :url)
                                   :name (calculator :name)})))

(defn count-non-zeros
  [tensor]
  (reduce
    (fn [c dimension]
      (if (zero? dimension)
          c
          (+ c 1)))
    0
    tensor))

(defn get-sorting-order
  [{similarities :similarities}]
  (vector
    (:name similarities)
    (:url  similarities)
    (:body similarities)))

(defn rank-documents
  [documents query-tensor]
  (reverse
    (sort-by
      get-sorting-order
      (map #(set-similarities % query-tensor) documents))))
