(ns marklens.tasks.index
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [marklens.crawler :as crawler]
            [marklens.etl.chrome.file-reader :as file-reader]
            [marklens.indexer :as indexer]
            [marklens.storage :as storage]
            [marklens.tasks.builder :as builder]))

(defn- load-pages [{file :file}]
  {:loaded-pages (file-reader/get-pages-from-file-2 file)})

(defn- filter-unseen-pages [{loaded-pages :loaded-pages
                             parsed-urls :already-parsed-urls}]
  {:parsed-pages (indexer/filter-unseen-pages loaded-pages parsed-urls)})

(defn- crawl-pages [{pages :parsed-pages
                     url-crawler :url-crawler
                     stop-words :stop-words}]
  {:crawled-pages (indexer/pages-to-tokens pages url-crawler stop-words)})

(defn- save-pages [{crawled-pages :crawled-pages
                    con :con}]
  {:saved-pages
   (doall
    (map
     #(indexer/save-page!
       con
       (:page %)
       (:tokens %))
     crawled-pages))})

(defn index
  [file-name]
  (jdbc/with-db-transaction [t-con storage/db-spec]
    ((builder/create-task
      load-pages
      filter-unseen-pages
      crawl-pages
      save-pages)
     {:file (slurp file-name)
      :already-parsed-urls (storage/get-parsed-urls!)
      :url-crawler crawler/text-from-page!
      :stop-words (set (string/split-lines (slurp "resources/stopwords.txt")))
      :con t-con})))

; (def loaded-pages (load-pages {:file (slurp "/home/gabriel/.config/BraveSoftware/Brave-Browser/Default/Bookmarks")}))
; loaded-pages
; (def as-json (json/read-json (slurp "/home/gabriel/.config/BraveSoftware/Brave-Browser/Default/Bookmarks")))
; (def content (crawler/text-from-page! "https://reikidosantaines.site"))
; (index "/home/gabriel/.config/BraveSoftware/Brave-Browser/Default/Bookmarks")
