(ns marklens.core
  (:gen-class)
  (:require [clojure.string :as string]
            [marklens.search-engine.searcher :as searcher]
            [marklens.storage :as storage]
            [marklens.tasks :as tasks]))

(defn get-help
  []
  (println (slurp "resources/help.txt")))

(defn print-results
  [results]
  (doseq [[index result] (reverse (map-indexed #(vector %1 %2) results))]
    (println
     (str (+ 1 index)
          ". " (:name result)
          "\n   URL: " (:url result)
          "\n   Folder: " (clojure.string/join
                           " > "
                           (reverse (:parents result)))
          "\n"))))

(defn -main
  [& all-args]
  (storage/initialize-db!)
  (let [[command & args] all-args]
    (cond
      (= command "index") (if-not (= (count args) 1)
                            (println "The json's filepath must come after the command 'index'. Use the command 'help' for more information.")
                            (tasks/index (first args)))
      (= command "search") (if (empty? args)
                             (println "Add the search query after the command 'search'. Use the command 'help' for more information.")
                             (print-results (searcher/search (string/join " " args))))
      (= command "help") (get-help)
      :else (get-help))))
