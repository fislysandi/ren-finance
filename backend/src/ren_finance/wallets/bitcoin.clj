(ns ren-finance.wallets.bitcoin
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [ren-finance.wallets.protocol :refer [WalletLookup]]
            [ren-finance.wallets.normalizer :refer [normalize-balance valid-btc-address? valid-btc-bech32-address?]]))

(defn- valid-btc-address?*
  [address]
  (or (valid-btc-address? address) (valid-btc-bech32-address? address)))

(defrecord BitcoinWallet [address]
  WalletLookup
  (fetch-balance [this]
    (if-not (valid-btc-address?* address)
      (throw (IllegalArgumentException. (str "Invalid BTC address: " address))))
    (try
      (let [start (System/currentTimeMillis)
            url (str "https://blockchain.info/balance?active=" address)
            {:keys [status body]} (http/get url {:socket-timeout 5000 :conn-timeout 3000 :as :json})
            elapsed (- (System/currentTimeMillis) start)]
        (println (str "[wallet] BTC balance fetch for " address " - status: " status " " elapsed "ms"))
        (if (= 200 status)
          (let [data (get body address)]
            {:asset :BTC
             :balance (bigdec (or (/ (get data "final_balance" 0) 100000000.0) 0))
             :address address})
          (throw (RuntimeException. (str "HTTP error: " status)))))
      (catch java.net.SocketTimeoutException e
        (println (str "[wallet] Timeout fetching BTC balance for " address))
        {:asset :BTC, :balance 0M, :address address, :error :timeout})
      (catch Exception e
        (println (str "[wallet] Error fetching BTC balance for " address ": " (.getMessage e)))
        {:asset :BTC, :balance 0M, :address address, :error :api-error})))

  (fetch-transactions [this limit]
    [])

  (wallet-address [this] address)

  (chain-type [this] :BTC))

(defn make-wallet
  "Create a Bitcoin wallet lookup instance."
  [address]
  (->BitcoinWallet address))
