(ns marklens.etl.firefox
  (:require [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [java-time :as java-time]
            [marklens.utils :as utils]))

(defn- query-nodes!
  "Queries every tree node, folders and pages."
  [db-location]
  (let [query "SELECT
                b.id,
                b.title as name,
                CASE b.type
                  WHEN 1 THEN 'url'
                  ELSE 'folder'
                END as type,
                b.parent,
                b.dateAdded as date_added,
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

(defn- epoch-to-date-time [epoch]
  (let [base (java-time/local-date-time 1970 1 1)
        elapsed (/ epoch 1000)]
    (java-time/plus base (java-time/duration elapsed))))

(defn- get-entry-parents [entry-by-id entry]
  (let [parent (:parent entry)]
    (if (< parent 2)
      (list)
      (cons parent (get-entry-parents entry-by-id (get entry-by-id parent))))))

(defn- capitalize-default-names
  [entries]
  (map
   (fn [entry]
     (assoc entry :name (if (<= (:id entry) 6)
                          (string/capitalize (:name entry))
                          (:name entry))))
   entries))

(defn- remove-unwanted-keys [entries]
  (map #(dissoc % :id :type :parent) entries))

(defn- conformer [entries]
  (let [entry-by-id (utils/index-by :id (capitalize-default-names entries))]
    (println entry-by-id)
    (map
     (fn [[_ entry]]
       (assoc entry
              :date_added
              (epoch-to-date-time (:date_added entry))
              :parents
              (map
               #(:name (get entry-by-id %))
               (get-entry-parents entry-by-id entry))))
     entry-by-id)))

(defn get-pages-from-db
  "Reads bookmark sqlite file and returns a list of pages"
  [db-path]
  (remove-unwanted-keys
   (filter #(= (:type %) "url")
           (conformer (query-nodes! db-path)))))

; (def nodes (query-nodes! "/home/gabriel/.mozilla/firefox/npdximv4.default-release/places.sqlite"))
; (def nodes-2 (get-pages-from-db "/home/gabriel/.mozilla/firefox/npdximv4.default-release/places.sqlite"))
; (take 2 nodes-2)
; (take 20 nodes)
; (doall (conformer (take 20 nodes)))
; (def node-by-id (utils/index-by :id nodes))
; (map #(get-entry-parents node-by-id %) (take 10 nodes))
; (take 2 nodes)
