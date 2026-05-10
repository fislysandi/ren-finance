(ns ren-finance.integration-test
  (:require [clojure.test :refer :all]
            [ren-finance.db.conn :as conn]
            [ren-finance.db.schema :as schema]
            [ren-finance.db.migrations :as migrations]
            [ren-finance.wallets.ethereum :as eth]
            [ren-finance.wallets.bitcoin :as btc]
            [ren-finance.wallets.protocol :refer [WalletLookup fetch-balance]]
            [ren-finance.wallets.normalizer :refer [valid-eth-address? valid-btc-address?]]
            [ren-finance.import.csv :as csv-import]
            [ren-finance.currency.converter :as converter]
            [ren-finance.queries.net-worth :as net-worth]
            [ren-finance.queries.transactions :as tx-queries]
            [datahike.api :as d]
            [clojure.java.io :as io]))

(def ^:dynamic *conn* nil)

(defn setup-db [test-fn]
  (let [tmp-dir (str "target/test-db-" (System/currentTimeMillis))
        cfg {:store {:backend :file :path tmp-dir}
             :keep-history? true
             :schema-flexibility :read}]
    (try
      (d/create-database cfg)
      (let [conn (d/connect cfg)]
        ;; Transact schema
        @(d/transact conn schema/finance-schema)
        ;; Seed categories
        (migrations/run-migrations! conn)
        (binding [*conn* conn]
          (test-fn))
        (d/release conn))
      (finally
        (d/delete-database cfg)))))

(use-fixtures :each setup-db)

(deftest test-address-validation
  (testing "Valid Ethereum address"
    (is (valid-eth-address? "0x742d35Cc6634C0532925a3b844Bc9e7595f2bD18")))
  (testing "Invalid Ethereum address"
    (is (not (valid-eth-address? "invalid-address"))))
  (testing "Valid Bitcoin address (legacy)"
    (is (valid-btc-address? "1A1zP1eP5QGefi2DMPTfTL5SLmv7DivfNa")))
  (testing "Invalid Bitcoin address"
    (is (not (valid-btc-address? "bad-address")))))

(deftest test-net-worth-empty
  (testing "Net worth is 0 with no accounts"
    (is (= 0M (net-worth/net-worth *conn*)))
    (is (= [] (net-worth/net-worth-by-type *conn*)))
    (is (= [] (net-worth/all-accounts *conn*)))))

(deftest test-net-worth-with-accounts
  (testing "Net worth calculation with accounts"
    @(d/transact *conn*
      [{:db/id -1 :account/name "Checking" :account/type :checking :account/balance 5000M :account/currency :USD}
       {:db/id -2 :account/name "Savings" :account/type :savings :account/balance 10000M :account/currency :USD}
       {:db/id -3 :account/name "Crypto Wallet" :account/type :crypto :account/balance 2.5M :account/currency :BTC}])
    (is (= 15002.5M (net-worth/net-worth *conn*)))
    (let [by-type (net-worth/net-worth-by-type *conn*)]
      (is (= 3 (count by-type)))
      (is (some #(= :checking (:type %)) by-type))
      (is (some #(= 5000M (:balance %)) (filter #(= :checking (:type %)) by-type))))))

(deftest test-transaction-query
  (testing "Query empty transactions"
    (let [result (tx-queries/query-transactions *conn* {:limit 10})]
      (is (= 0 (:total result)))
      (is (= [] (:items result))))))

(deftest test-csv-import-basic
  (testing "Import generic CSV"
    (let [csv-content "date,amount,description\n2026-01-01,-2500,Rent\n2026-01-02,5000,Salary\n"
          tmp-file (java.io.File/createTempFile "test-import" ".csv")
          _ (spit tmp-file csv-content)
          result (csv-import/import-csv (.getAbsolutePath tmp-file) {})]
      (is (= 2 (:imported result)))
      (is (= 0 (:duplicates result)))
      (is (= [] (:errors result)))
      (.delete tmp-file))))

(deftest test-csv-import-dedup
  (testing "Import same CSV twice — no duplicates"
    (let [csv-content "date,amount,description\n2026-01-01,-2500,Rent\n2026-01-02,5000,Salary\n"
          tmp-file (java.io.File/createTempFile "test-dedup" ".csv")
          _ (spit tmp-file csv-content)]
      ;; First import
      (csv-import/import-csv (.getAbsolutePath tmp-file) {})
      ;; Second import
      (let [result (csv-import/import-csv (.getAbsolutePath tmp-file) {})]
        (is (= 0 (:imported result)))
        (is (= 2 (:duplicates result))))
      (.delete tmp-file))))

(deftest test-csv-import-errors
  (testing "Import CSV with parse errors — all-or-nothing"
    (let [csv-content "date,amount,description\nnot-a-date,100,Test\n"
          tmp-file (java.io.File/createTempFile "test-errors" ".csv")
          _ (spit tmp-file csv-content)
          result (csv-import/import-csv (.getAbsolutePath tmp-file) {})]
      (is (= 0 (:imported result)))
      (is (pos? (count (:errors result))))
      (.delete tmp-file))))

(deftest test-currency-conversion
  (testing "Convert same currency — identity"
    (is (= 100M (converter/convert 100M :USD :USD)))))

(deftest test-mcp-tools-exist
  (testing "MCP tool definitions exist"
    (require 'ren-finance.mcp.tools)
    (let [tools (find-var 'ren-finance.mcp.tools/all-tools)]
      (is (some? @tools))
      (is (= 4 (count @tools)))
      (is (some #(= "get_net_worth" (:name %)) @tools))
      (is (some #(= "fetch_wallet_data" (:name %)) @tools))
      (is (some #(= "list_transactions" (:name %)) @tools))
      (is (some #(= "generate_report" (:name %)) @tools)))))
