(ns ren-finance.wallets.normalizer)

(defn normalize-balance
  "Normalize API response to unified balance shape.
   Input: map with :asset, :balance, :address keys
   Output: {:asset keyword, :balance BigDecimal, :address string, :normalized-at inst}"
  [{:keys [asset balance address]}]
  {:asset (keyword asset)
   :balance (bigdec balance)
   :address address
   :normalized-at (java.util.Date.)})

(defn normalize-transaction
  "Normalize API transaction to unified shape."
  [{:keys [tx-id amount from to timestamp asset]}]
  {:tx-id (str tx-id)
   :amount (bigdec amount)
   :from (str from)
   :to (str to)
   :timestamp (if (instance? java.util.Date timestamp) timestamp (java.util.Date.))
   :asset (keyword asset)})

(defn valid-eth-address?
  "Validate Ethereum address format (0x + 40 hex chars)."
  [address]
  (boolean (re-matches #"^0x[a-fA-F0-9]{40}$" address)))

(defn valid-btc-address?
  "Validate Bitcoin address formats (legacy, segwit, bech32)."
  [address]
  (boolean (re-matches #"^[13][a-km-zA-HJ-NP-Z1-9]{25,34}$" address)))

(defn valid-btc-bech32-address?
  "Validate Bitcoin bech32 address format."
  [address]
  (boolean (re-matches #"^bc1[a-zA-Z0-9]{39,59}$" address)))
