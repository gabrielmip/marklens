(ns marklens.storage
  (:require [clojure.data.json :as json]
            [clojure.java.jdbc :as jdbc]
            [clojure.string :as string]
            [marklens.utils :as utils]))

(def db-spec
  {:classname "org.sqlite.JDBC"
   :subprotocol "sqlite"
   :subname "resources/db.sqlite3"})

; referência: http://clojure-doc.org/articles/ecosystem/java_jdbc/reusing_connections.html
; (defn pool
;   [spec]
;   (let [cpds (doto (ComboPooledDataSource.)
;                (.setDriverClass (:classname spec))
;                (.setJdbcUrl (str "jdbc:" (:subprotocol spec) ":" (:subname spec)))
;               ;  (.setUser (:user spec))
;               ;  (.setPassword (:password spec))
;                ;; expire excess connections after 30 minutes of inactivity:
;                (.setMaxIdleTimeExcessConnections (* 30 60))
;                ;; expire connections after 3 hours of inactivity:
;                (.setMaxIdleTime (* 3 60 60)))]
;     {:datasource cpds}))

; (def pooled-db (delay (pool db-spec)))

; (defn db-connection [] @pooled-db)

(defn- create-terms-table! []
  (jdbc/create-table-ddl :terms
                         [[:term "varchar(255) unique"]]
                         {:conditional? true}))

(defn- create-documents-table! []
  (jdbc/create-table-ddl :documents
                         [[:name "varchar(255)"]
                          [:url "text unique"]
                          [:date_added :datetime]
                          [:parents :text] ; json
                          [:nwords :int]
                          [:content :text]]
                         {:conditional? true}))

(defn- create-frequency-table! []
  (jdbc/create-table-ddl :term_frequency
                         [[:term_id :int]
                          [:document_id :int]
                          [:frequency :int]
                          [:origin "varchar(30)"]
                          [:indexes :text] ; json
                          ["FOREIGN KEY(term_id) REFERENCES terms(rowid)"]
                          ["FOREIGN KEY(document_id) REFERENCES documents(rowid)"]]
                         {:conditional? true}))

(defn initialize-db! []
  (try (jdbc/db-do-commands
        db-spec
        [(create-documents-table!)
         (create-terms-table!)
         (create-frequency-table!)
         "CREATE INDEX term_id_document_id ON term_frequency(term_id, document_id)"])
       (catch java.sql.BatchUpdateException _)))

(defn drop-db! []
  (try (jdbc/db-do-commands
        db-spec
        [(jdbc/drop-table-ddl :term_frequency)
         (jdbc/drop-table-ddl :terms)
         (jdbc/drop-table-ddl :documents)])
       (catch Exception e
         (println (.getMessage e)))))

(defn- update-or-insert!
  "Updates columns or inserts a new row in the specified table"
  [con table row where-clause]
  (try (jdbc/insert! con table row)
       (catch Exception _ ; expects exceptions when tuple already exists
         (jdbc/update! con table row where-clause))))


(defn- query-element-id!
  [con table pk-field pk-value]
  (let [query (string/join ["select rowid from " (name table) " where " (name pk-field) " = ? limit 1"])]
    (:rowid
     (first
      (jdbc/query con [query pk-value])))))

(defn insert-document!
  [con document]
  (let [jsonified-parents (json/json-str (:parents document))
        url (:url document)
        to-be-inserted (assoc document :parents jsonified-parents)]
    (update-or-insert! con :documents to-be-inserted ["url = ?" url])
    (query-element-id! con :documents :url url)))

(defn insert-terms!
  [con terms]
  (let [entity-builder (fn [term] {:term term})]
    (doall
     (map
      (fn [term]
        (update-or-insert! con :terms (entity-builder term) ["term = ?" term])
        (query-element-id! con :terms :term term))
      terms))))

(defn- insert-frequency!
  [con document token]
  (let [term-id (:rowid token)
        document-id (:rowid document)
        origin (:origin token)
        entity {:document_id document-id
                :term_id term-id
                :frequency (:frequency token)
                :origin origin
                :indexes (json/json-str (:indexes token))}]
    (update-or-insert!
     con
     :term_frequency
     entity
     ["term_id = ? and document_id = ? and origin = ?" term-id document-id origin])
    true))

(defn insert-frequencies!
  [con document tokens]
  (doall
   (map
    #(insert-frequency! con document %)
    tokens)))

(defn get-parsed-urls! []
  (set
   (jdbc/query db-spec ["select url from documents"]
               {:row-fn :url})))

(defn query-doc-term-stats!
  "Returns the IDs from term, document, origin and
   frequency from provided term IDs."
  [term-ids]
  (jdbc/query db-spec
              (cons
               (str "select
              term_id,
              document_id,
              origin,
              frequency
            from term_frequency
            where term_id in ("
                    (string/join "," (repeat (count term-ids) "?"))
                    ")")
               (map str term-ids))))

(defn query-term-stats!
  [term-ids]
  (jdbc/query db-spec
              (cons
               (str "select
              term_id,
              count(document_id) as ndocs,
              d.total_ndocs
            from term_frequency
              join (select count(*) as total_ndocs from documents) as d
            where term_id in ("
                    (string/join "," (repeat (count term-ids) "?"))
                    ")
            group by term_id")
               (map str term-ids))))

(defn get-term-ids-by-term!
  [terms]
  (utils/index-value-by
   :rowid
   :term
   (jdbc/query db-spec
               (cons
                (str "select
                rowid,
                term
              from terms
              where term in ("
                     (string/join "," (repeat (count terms) "?"))
                     ")")
                terms))))

(defn query-term-ids!
  "returns the corresponding ids in the same order.
   returns 0 when the term is not defined in our vocabulary."
  [terms]
  (let [ids-by-term (get-term-ids-by-term! terms)]
    (map #(get ids-by-term % 0) terms)))

(defn parse-document-jsonified-fields
  [document]
  (assoc document :parents (json/read-str (:parents document))))

(defn get-indexed-documents!
  [ids]
  (utils/index-by
   :rowid
   (map
    parse-document-jsonified-fields
    (jdbc/query db-spec
                (cons
                 (str "select rowid, name, url, parents, date_added
                from documents
                where rowid in ("
                      (string/join "," (repeat (count ids) "?"))
                      ")")
                 ids)))))
