(ns marklens.search-engine.ranking)

(defn get-tensor-norm
  [tensor]
  (let [square #(* % %)]
    (Math/sqrt (apply + (map square tensor)))))

(defn set-tensor-norm
  [document]
  (assoc document :norm (get-tensor-norm (:tensor document))))

(defn rank-documents
  [documents]
  (reverse
    (sort-by
      :norm
      (map set-tensor-norm documents))))
