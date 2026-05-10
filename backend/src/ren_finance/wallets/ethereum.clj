(ns ren-finance.wallets.ethereum
  (:require [clj-http.client :as http]
            [clojure.edn :as edn]
            [ren-finance.wallets.protocol :refer [WalletLookup]]
            [ren-finance.wallets.normalizer :refer [normalize-balance valid-eth-address?]]))

(defrecord EthereumWallet [address]
  WalletLookup
  (fetch-balance [this]
    (if-not (valid-eth-address? address)
      (throw (IllegalArgumentException. (str "Invalid ETH address: " address))))
    (try
      (let [start (System/currentTimeMillis)
            url (str "https://api.etherscan.io/api?module=account&action=balance&address=" address "&tag=latest")
            {:keys [status body]} (http/get url {:socket-timeout 5000 :conn-timeout 3000})
            elapsed (- (System/currentTimeMillis) start)]
        (println (str "[wallet] ETH balance fetch for " address " - status: " status " " elapsed "ms"))
        (if (= 200 status)
          (let [data (edn/read-string body)]
            (if (= "1" (get data "status" "0"))
              {:asset :ETH
               :balance (bigdec (get data "result" "0"))
               :address address}
              (throw (RuntimeException. (str "Etherscan error: " (get data "message"))))))
          (throw (RuntimeException. (str "HTTP error: " status)))))
      (catch java.net.SocketTimeoutException e
        (println (str "[wallet] Timeout fetching ETH balance for " address))
        {:asset :ETH, :balance 0M, :address address, :error :timeout})
      (catch Exception e
        (println (str "[wallet] Error fetching ETH balance for " address ": " (.getMessage e)))
        {:asset :ETH, :balance 0M, :address address, :error :api-error})))

  (fetch-transactions [this limit]
    ;; MVP: return empty — full tx history is future scope
    [])

  (wallet-address [this] address)

  (chain-type [this] :ETH))

(defn make-wallet
  "Create an Ethereum wallet lookup instance."
  [address]
  (->EthereumWallet address))
