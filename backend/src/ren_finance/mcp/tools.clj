(ns ren-finance.mcp.tools
  (:require [ren-finance.db.conn :as conn]
            [ren-finance.queries.net-worth :as net-worth]
            [ren-finance.queries.transactions :as tx-queries]
            [ren-finance.wallets.ethereum :as eth]
            [ren-finance.wallets.bitcoin :as btc]
            [datahike.api :as d]
            [clojure.string :as str])
  (:import [java.text SimpleDateFormat]
           [java.util Date]))

(defn- format-amount
  "Format BigDecimal amount with 2 decimal places."
  [amount]
  (if amount
    (format "%.2f" (double amount))
    "0.00"))

(defn- format-date
  "Format Date to ISO string."
  [d]
  (if (instance? Date d)
    (.format (SimpleDateFormat. "yyyy-MM-dd'T'HH:mm:ss'Z'") d)
    (str d)))

(defn- parse-date
  "Parse ISO date string to java.util.Date."
  [s]
  (when s
    (.parse (SimpleDateFormat. "yyyy-MM-dd") s)))

(def all-tools
  "List of 4 read-only MCP tool definitions."
  [{:name "get_net_worth"
    :title "Get Net Worth"
    :description "Get current net worth breakdown including account types and last sync time. Returns total net worth, breakdown by type (checking, savings, crypto, etc.), and asset totals by currency."
    :schema {}
    :tool-fn
    (fn [req args]
      (try
        (let [c (conn/get-conn)
              total (net-worth/net-worth c)
              by-type (net-worth/net-worth-by-type c)
              assets (net-worth/asset-totals c)
              accounts (net-worth/all-accounts c)]
          {:content [{:type "text"
                      :text (str "Net Worth: $" (format-amount total) "\n\n"
                                 "By Account Type:\n"
                                 (str/join "\n" (map #(str "  " (name (:type %)) ": $" (format-amount (:balance %))) by-type))
                                 "\n\nBy Asset:\n"
                                 (str/join "\n" (map #(str "  " (name (:currency %)) ": $" (format-amount (:balance %))) assets))
                                 "\n\nTotal Accounts: " (count accounts)
                                 "\nLast Sync: " (format-date (Date.)))}]
           :isError false})
        (catch Exception e
          {:content [{:type "text" :text (str "Error fetching net worth: " (.getMessage e))}]
           :isError true})))}

   {:name "fetch_wallet_data"
    :title "Fetch Wallet Data"
    :description "Trigger wallet address sync to refresh balances. Provide wallet addresses to fetch their current balances from public blockchain APIs (Etherscan for ETH, Blockchain.com for BTC)."
    :schema {:type "object"
             :properties {:addresses {:type "array"
                                      :items {:type "string"}
                                      :description "List of wallet addresses to sync"}}}
    :tool-fn
    (fn [req args]
      (try
        (let [addresses (or (get args "addresses") (get args :addresses) [])
              results (mapv (fn [addr]
                              (let [wallet (if (re-find #"^0x" addr)
                                             (eth/make-wallet addr)
                                             (btc/make-wallet addr))
                                    balance (try
                                              (.fetch-balance wallet)
                                              (catch Exception _
                                                {:asset :UNKNOWN :balance 0M :address addr}))]
                                balance))
                            addresses)]
          {:content [{:type "text"
                      :text (str "Wallet Sync Results:\n"
                                 (str/join "\n" (map #(str "  " (:address %) " (" (name (:asset %)) "): " (:balance %)) results)))}]
           :isError false})
        (catch Exception e
          {:content [{:type "text" :text (str "Error syncing wallets: " (.getMessage e))}]
           :isError true})))}

   {:name "list_transactions"
    :title "List Transactions"
    :description "List recent transactions with optional filters. Supports limit, category filter, and date range (from/to). Returns newest transactions first."
    :schema {:type "object"
             :properties {:limit {:type "number" :description "Max transactions to return (default 20)"}
                          :category {:type "string" :description "Filter by category name"}
                          :from {:type "string" :description "Start date (ISO format YYYY-MM-DD)"}
                          :to {:type "string" :description "End date (ISO format YYYY-MM-DD)"}}}
    :tool-fn
    (fn [req args]
      (try
        (let [limit (or (some-> (get args "limit") int) (some-> (get args :limit) int) 20)
              cat (or (get args "category") (get args :category))
              from (some-> (or (get args "from") (get args :from)) parse-date)
              to (some-> (or (get args "to") (get args :to)) parse-date)
              opts (merge {:limit limit}
                          (when cat {:category (keyword cat)})
                          (when (and from to) {:from from :to to}))
              result (tx-queries/query-transactions (conn/get-conn) opts)]
          {:content [{:type "text"
                      :text (str "Transactions (" (:total result) " total):\n"
                                 (str/join "\n" (map #(str "  " (format-date (:date %)) " | $" (format-amount (:amount %)) " | " (:description %) " | " (:category %)) (:items result))))}]
           :isError false})
        (catch Exception e
          {:content [{:type "text" :text (str "Error listing transactions: " (.getMessage e))}]
           :isError true})))}

   {:name "generate_report"
    :title "Generate Report"
    :description "Generate spending and income report for a date range. Returns total spending, total income, net change, and breakdown by category for the specified period."
    :schema {:type "object"
             :properties {:start-date {:type "string" :description "Start date (YYYY-MM-DD)"}
                          :end-date {:type "string" :description "End date (YYYY-MM-DD)"}}
             :required ["start-date" "end-date"]}
    :tool-fn
    (fn [req args]
      (try
        (let [start-date (some-> (or (get args "start-date") (get args :start-date)) parse-date)
              end-date (some-> (or (get args "end-date") (get args :end-date)) parse-date)]
          (if (and start-date end-date)
            (let [c (conn/get-conn)
                  spending (net-worth/spending-by-category c start-date end-date)
                  total-spent (reduce + 0M (map :total spending))
                  total-income (or (ffirst (d/q '[:find (sum ?amount)
                                                  :in $ ?start ?end
                                                  :where [?t :transaction/date ?date]
                                                         [(>= ?date ?start)]
                                                         [(<= ?date ?end)]
                                                         [?t :transaction/amount ?amount]
                                                         [(> ?amount 0)]]
                                                @c start-date end-date)) 0M)]
              {:content [{:type "text"
                          :text (str "Report: " (or (get args "start-date") (get args :start-date)) " → " (or (get args "end-date") (get args :end-date)) "\n"
                                     "  Total Income:  $" (format-amount total-income) "\n"
                                     "  Total Spending: $" (format-amount total-spent) "\n"
                                     "  Net Change:    $" (format-amount (- total-income total-spent)) "\n"
                                     "\nSpending by Category:\n"
                                     (str/join "\n" (map #(str "  " (:category %) ": $" (format-amount (:total %))) spending)))}]
               :isError false})
            {:content [{:type "text" :text "Error: Both start-date and end-date are required."}]
             :isError true}))
        (catch Exception e
          {:content [{:type "text" :text (str "Error generating report: " (.getMessage e))}]
           :isError true}))}])
