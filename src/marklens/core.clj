(ns marklens.core
  (:gen-class)
  (:require [marklens.file-reader :as file-reader]
            [marklens.storage :as storage]
            [marklens.crawler :as crawler]
            [marklens.indexer :as indexer]
            [clojure.pprint :as pprint]))

(defn -main
  [file-name]
  (storage/initialize-db!)
  (indexer/index-pages! (file-reader/get-pages-from-file file-name)))
