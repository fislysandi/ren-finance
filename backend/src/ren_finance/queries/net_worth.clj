(ns ren-finance.queries.net-worth
  (:require [datahike.api :as d]
            [ren-finance.db.conn :as conn]))

(defn net-worth
  "Calculate total net worth — sum of all account balances.
   Returns BigDecimal."
  ([]
   (net-worth (conn/get-conn)))
  ([conn]
   (let [result (d/q '[:find (sum ?balance)
                       :where [?e :account/balance ?balance]]
                     @conn)]
     (or (ffirst result) 0M))))

(defn net-worth-by-type
  "Net worth breakdown by account type.
   Returns [{:type keyword :balance BigDecimal}].
   Types: :checking, :savings, :crypto, :cash, :credit"
  ([]
   (net-worth-by-type (conn/get-conn)))
  ([conn]
   (let [result (d/q '[:find ?type (sum ?balance)
                       :where [?e :account/type ?type]
                              [?e :account/balance ?balance]]
                     @conn)]
     (mapv (fn [[type balance]] {:type type :balance (or balance 0M)}) result))))

(defn net-worth-at
  "Historical net worth at given date using d/as-of.
   Requires :keep-history? true in Datahike config.
   date should be java.util.Date or java.time.Instant.
   Returns BigDecimal or nil if no data at that point."
  ([date]
   (net-worth-at (conn/get-conn) date))
  ([conn date]
   (try
     (let [db (d/as-of @conn date)]
       (-> (d/q '[:find (sum ?balance)
                  :where [?e :account/balance ?balance]]
                db)
           ffirst))
     (catch Exception e
       (println (str "[net-worth] Historical query failed: " (.getMessage e)))
       nil))))

(defn spending-by-category
  "Spending breakdown by category for a date range.
   start-date and end-date should be java.util.Date or java.time.Instant.
   Returns [{:category string :total BigDecimal}]."
  ([start-date end-date]
   (spending-by-category (conn/get-conn) start-date end-date))
  ([conn start-date end-date]
   (let [result (d/q '[:find ?cat-name (sum ?amount)
                       :in $ ?start ?end
                       :where [?t :transaction/date ?date]
                              [(>= ?date ?start)]
                              [(<= ?date ?end)]
                              [?t :transaction/amount ?amount]
                              [(< ?amount 0)]
                              [?t :transaction/category ?c]
                              [?c :category/name ?cat-name]]
                     @conn start-date end-date)]
     (mapv (fn [[cat total]] {:category cat :total (.abs total)}) result))))

(defn all-accounts
  "Get all accounts with details."
  ([]
   (all-accounts (conn/get-conn)))
  ([conn]
   (let [result (d/q '[:find ?e ?name ?type ?balance ?currency
                       :where [?e :account/name ?name]
                              [?e :account/type ?type]
                              [?e :account/balance ?balance]
                              [?e :account/currency ?currency]]
                     @conn)]
     (mapv (fn [[e name type balance currency]]
             {:db/id e :name name :type type :balance balance :currency currency})
           result))))

(defn asset-totals
  "Total net worth grouped by asset/currency (multi-currency breakdown)."
  ([]
   (asset-totals (conn/get-conn)))
  ([conn]
   (let [result (d/q '[:find ?currency (sum ?balance)
                       :where [?e :account/balance ?balance]
                              [?e :account/currency ?currency]]
                     @conn)]
     (mapv (fn [[currency balance]] {:currency currency :balance (or balance 0M)}) result))))
