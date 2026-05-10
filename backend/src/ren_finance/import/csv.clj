(ns ren-finance.import.csv
  (:require [clojure.data.csv :as csv]
            [clojure.java.io :as io]
            [clojure.string :as str]
            [datahike.api :as d]
            [ren-finance.db.conn :as conn]))

;; ─── Date Parsing ───────────────────────────────────────────────────

(def ^:private date-formats
  "Ordered list of date formats to try, from most specific to most generic."
  ["yyyy-MM-dd HH:mm:ss"
   "yyyy-MM-dd"
   "MM/dd/yyyy"
   "dd/MM/yyyy"
   "yyyy/MM/dd"])

(defn- parse-date
  "Try multiple date formats against date-str. Returns java.util.Date or nil."
  [date-str]
  (when date-str
    (let [trimmed (str/trim date-str)]
      (some (fn [fmt]
              (try
                (-> (java.text.SimpleDateFormat. fmt)
                    (doto (.setLenient false))
                    (.parse trimmed))
                (catch Exception _ nil)))
            date-formats))))

;; ─── Amount Parsing ─────────────────────────────────────────────────

(defn- parse-amount
  "Parse amount string to BigDecimal.
   Strips commas as thousands separator.
   Returns 0M on unparseable input."
  [amount-str]
  (when amount-str
    (try
      (bigdec (str/trim (str/replace amount-str "," "")))
      (catch Exception _ 0M))))

;; ─── Import Hash ────────────────────────────────────────────────────

(defn- compute-import-hash
  "SHA-256 hex hash of date+amount+description+account for dedup.
   Same values → same hash → detected as duplicate on re-import."
  [date amount description account]
  (let [input (str date "|" amount "|" description "|" account)]
    (->> (.getBytes input "UTF-8")
         (java.security.MessageDigest/getInstance "SHA-256")
         (.digest)
         (map (fn [b] (format "%02x" (bit-and b 0xff))))
         (apply str))))

;; ─── Row Parsing ────────────────────────────────────────────────────

(defn- parse-csv-row
  "Parse a single CSV row into transaction data map.
   Returns {:date ... :amount ... :description ... :account ... :import-hash ...}
   or {:error msg} on failure."
  [row headers mapping]
  (try
    (let [date-col    (or (:date mapping) "date")
          amount-col  (or (:amount mapping) "amount")
          desc-col    (or (:description mapping) "description")
          acct-col    (or (:account mapping) "account")
          row-map     (zipmap headers row)
          date-str    (get row-map date-col)
          amount-str  (get row-map amount-col)
          desc        (get row-map desc-col)
          account     (get row-map acct-col "Imported")]
      (if-let [parsed-date (parse-date date-str)]
        (let [amount (parse-amount amount-str)]
          {:date        parsed-date
           :amount      amount
           :description (or desc "Imported transaction")
           :account     account
           :import-hash (compute-import-hash parsed-date amount desc account)})
        {:error (str "Invalid date format: '" date-str "'")}))
    (catch Exception e
      {:error (.getMessage e)})))

;; ─── Transaction Helpers ────────────────────────────────────────────

(defn- row->tx-entity
  "Convert parsed row map to Datahike transaction entity map.
   Uses negative temp-id for new entities."
  [row]
  {:transaction/date          (:date row)
   :transaction/amount        (:amount row)
   :transaction/description   (:description row)
   :transaction/type          (if (neg? (.compareTo (:amount row) 0M))
                                :expense
                                :income)
   :transaction/import-hash   (:import-hash row)
   :transaction/import-source :csv})

;; ─── Main Import ────────────────────────────────────────────────────

(defn import-csv
  "Import CSV file into Datahike with deduplication.

   Parameters:
     file-path - path to CSV file
     mapping   - column mapping: {:date colname, :amount colname,
                                   :description colname, :account colname}
                 Defaults: date/amount/description/account

   Returns: {:imported int, :duplicates int, :errors [str]}

   Idempotent: re-running with the same file produces 0 imported,
   all rows counted as duplicates.

   All-or-nothing: if any row fails parsing, nothing is imported."
  [file-path mapping]
  (let [db-conn (conn/get-conn)]
    (with-open [reader (io/reader file-path)]
      (let [[header & rows] (csv/read-csv reader)
            parsed          (mapv #(parse-csv-row % header mapping) rows)
            errors          (filter :error parsed)
            valid           (remove :error parsed)]
        (if (seq errors)
          {:imported   0
           :duplicates 0
           :errors     (mapv :error errors)}
          (let [hashes         (set (map :import-hash valid))
                existing       (->> (d/q '[:find ?h
                                           :in $ [?h ...]
                                           :where [?e :transaction/import-hash ?h]]
                                         @db-conn hashes)
                                    (map first)
                                    (into #{}))
                new-rows       (remove #(existing (:import-hash %)) valid)
                duplicate-count (- (count valid) (count new-rows))]
            (when (seq new-rows)
              (try
                @(d/transact db-conn (mapv row->tx-entity new-rows))
                (catch Exception e
                  {:imported   0
                   :duplicates 0
                   :errors     [(.getMessage e)]})))
            {:imported   (count new-rows)
             :duplicates duplicate-count
             :errors     []}))))))
