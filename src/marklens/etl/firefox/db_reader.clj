(ns marklens.etl.firefox.db-reader
  (:require [clojure.java.jdbc :as jdbc]))

(defn query-nodes!
  "Queries every tree node, folders and pages.
   Folders have type 2. Pages, type 1."
  [db-location]
  (let [query "SELECT
                b.id,
                b.title,
                b.type,
                b.parent,
                p.url
              FROM
                moz_bookmarks b
              LEFT JOIN
                moz_places p
              ON
                b.fk = p.id"]
    (jdbc/query {:classname "org.sqlite.JDBC"
                 :subprotocol "sqlite"
                 :subname db-location}
                [query])))
