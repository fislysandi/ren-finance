(ns ren-finance.wallets.multi
  (:require [ren-finance.db.conn :as conn]
            [ren-finance.wallets.protocol :refer [WalletLookup]]
            [ren-finance.wallets.ethereum :as eth]
            [ren-finance.wallets.bitcoin :as btc]
            [clojure.tools.logging :as log]
            [datahike.api :as d]))

;; ---------------------------------------------------------------------------
;; Helpers
;; ---------------------------------------------------------------------------

(defn- make-wallet
  "Create appropriate wallet lookup instance from address format."
  [address chain-type]
  (case chain-type
    :ETH (eth/make-wallet address)
    :BTC (btc/make-wallet address)
    (throw (IllegalArgumentException.
            (str "Unknown chain type: " chain-type)))))

;; ---------------------------------------------------------------------------
;; Datahike queries (read-only)
;; ---------------------------------------------------------------------------

(defn get-configured-wallets
  "Get all configured wallet addresses from Datahike settings.
   Returns a set of [address chain-type label] tuples."
  []
  (let [db @(conn/get-conn)]
    (d/q '[:find ?address ?chain-type ?label
           :where [?e :wallet/address ?address]
                  [?e :wallet/chain-type ?chain-type]
                  [?e :wallet/label ?label]]
         db)))

;; ---------------------------------------------------------------------------
;; Sync: fetch fresh balances from all chains
;; ---------------------------------------------------------------------------

(defn sync-all-wallets
  "Fetch balances for ALL configured wallets sequentially.
   Rate-limits with 500ms pause between fetches.
   Returns [{:keys [address label asset chain-type balance error]}]."
  []
  (let [wallets (get-configured-wallets)]
    (log/info (str "Syncing " (count wallets) " wallets"))
    (doall
     (map-indexed
      (fn [i [address chain-type label]]
        (try
          (log/info (str "  [" (inc i) "/" (count wallets) "] "
                         label " (" address ")"))
          (let [wallet (make-wallet address chain-type)]
            (when (pos? i) (Thread/sleep 500))                ;; rate limit
            (let [result (.fetch-balance wallet)]
              (assoc result :label label :chain-type chain-type)))
          (catch Exception e
            (log/error e (str "Failed to sync wallet " address))
            {:address  address
             :label    label
             :chain-type chain-type
             :asset    :ERROR
             :balance  0M
             :error    (.getMessage e)})))
      wallets))))

(defn update-wallet-balances!
  "Store fetched balances back to Datahike using upsert semantics
   (:wallet/address is the unique identity)."
  [results]
  (let [conn (conn/get-conn)
        now  (java.util.Date.)]
    (doseq [{:keys [address balance asset]} results]
      (when (and address balance)
        (try
          @(d/transact conn
                       [{:wallet/address     address
                         :wallet/last-balance (bigdec balance)
                         :wallet/last-sync    now}])
          (log/info (str "  Updated " address " -> "
                         balance " " (name asset)))
          (catch Exception e
            (log/warn (str "  Failed to update " address
                           ": " (.getMessage e)))))))))

;; ---------------------------------------------------------------------------
;; Aggregation: sum balances across wallets
;; ---------------------------------------------------------------------------

(defn aggregate-balances
  "Aggregate balances by chain-type (asset) across all wallets.
   Sums last-balance for wallets of the same chain.
   Returns [{:asset keyword :total BigDecimal :count int}]."
  []
  (let [wallets (get-configured-wallets)
        totals  (atom {})]
    (doseq [[address _chain-type _label] wallets]
      (let [db     @(conn/get-conn)
            result (d/q '[:find ?balance ?asset
                          :in $ ?addr
                          :where [?e :wallet/address ?addr]
                                 [?e :wallet/last-balance ?balance]
                                 [?e :wallet/chain-type ?asset]]
                        db address)]
        (when-let [[balance asset] (first result)]
          (swap! totals update asset (fnil + 0M) (or balance 0M)))))
    (mapv (fn [[asset total]]
            (let [cnt (count (filter #(= (second %) asset) wallets))]
              {:asset asset :total total :count cnt}))
          @totals)))

;; ---------------------------------------------------------------------------
;; Net worth calculation
;; ---------------------------------------------------------------------------

(defn multi-wallet-net-worth
  "Calculate net worth from all wallets in base currency.
   Defaults to :USD; override via REN_DEFAULT_CURRENCY env var or explicit arg.
   Returns {:total BigDecimal :wallets [...] :base-currency keyword}."
  ([]
   (multi-wallet-net-worth
    (or (some-> (System/getenv "REN_DEFAULT_CURRENCY") keyword) :USD)))
  ([base-currency]
   (let [wallets  (get-configured-wallets)
         db       @(conn/get-conn)
         results  (doall
                   (map (fn [[address chain-type label]]
                          (let [balance (ffirst
                                         (d/q '[:find ?balance
                                                :in $ ?addr
                                                :where [?e :wallet/address ?addr]
                                                       [?e :wallet/last-balance ?balance]]
                                              db address))]
                            {:address    address
                             :chain-type chain-type
                             :label      label
                             :balance    (or balance 0M)}))
                        wallets))
         gross    (reduce + 0M (map :balance results))]
     {:total         gross
      :wallets       results
      :base-currency base-currency})))

;; ---------------------------------------------------------------------------
;; Full cycle: fetch  aggregate  persist
;; ---------------------------------------------------------------------------

(defn sync-and-aggregate!
  "Fetch fresh balances from all chains, persist to DB, return aggregate.
   Combines sync-all-wallets + update-wallet-balances! + aggregate-balances
   in a single call for MCP tool convenience."
  []
  (let [results    (sync-all-wallets)
        _          (update-wallet-balances! results)
        aggregated (aggregate-balances)]
    {:wallets    results
     :aggregated aggregated
     :timestamp  (java.util.Date.)}))
