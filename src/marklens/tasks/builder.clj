(ns marklens.tasks.builder)

(defn create-task [& fns]
  (fn [deps]
    (reduce
     #(merge %1 (%2 %1))
     deps
     fns)))
