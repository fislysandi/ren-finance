(ns ren-finance.wallets.protocol)

(defprotocol WalletLookup
  "Protocol for fetching balance and transaction data from blockchain wallets."
  (fetch-balance [this]
    "Fetch current balance for the wallet. Returns {:asset keyword, :balance BigDecimal, :address string}.")
  (fetch-transactions [this limit]
    "Fetch recent transactions. Returns [{:tx-id string, :amount BigDecimal, :from string, :to string, :timestamp inst, :asset keyword}].
     Limit parameter caps number of returned transactions.")
  (wallet-address [this]
    "Return the wallet's blockchain address as string.")
  (chain-type [this]
    "Return the blockchain type keyword (e.g. :ETH, :BTC)."))
