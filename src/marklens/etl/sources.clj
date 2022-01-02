(ns marklens.etl.sources
  (:require [marklens.etl.chrome :as chrome]
            [marklens.etl.firefox :as firefox]))

(def chrome chrome/get-pages-from-file-2)
(def firefox firefox/get-pages-from-db)
