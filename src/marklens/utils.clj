(ns marklens.utils)

(defn index-value-by
  "maps indexer-field to value-field"
  [value-field indexer-field values]
  (into {}
        (map
         (fn [item] [(indexer-field item) (value-field item)])
         values)))

(defn index-by
  [field items]
  (reduce
   (fn [cur item]
     (assoc cur (field item) item))
   {}
   items))
