(ns ren-finance.currency.converter
  (:require [clj-http.client :as http]
            [cheshire.core :as json]
            [clojure.string :as str]
            [datahike.api :as d]
            [ren-finance.db.conn :as conn])
  (:import [java.math MathContext RoundingMode]))

(def ^:private math-context (MathContext. 10 RoundingMode/HALF_UP))

(defn- fetch-fiat-rates
  "Fetch exchange rates from open.er-api.com. Returns {target-currency rate} or nil."
  [base-currency]
  (try
    (let [url (str "https://open.er-api.com/v6/latest/" (name base-currency))
          {:keys [status body]} (http/get url {:socket-timeout 5000 :conn-timeout 3000 :as :json})]
      (when (= 200 status)
        (get body "rates")))
    (catch Exception e
      (println (str "[currency] Failed to fetch fiat rates: " (.getMessage e)))
      nil)))

(defn- fetch-crypto-prices
  "Fetch crypto prices from CoinGecko. Returns {asset-keyword price-in-base} or nil."
  [base-currency]
  (try
    (let [url (str "https://api.coingecko.com/api/v3/simple/price?ids=bitcoin,ethereum&vs_currencies=" (name base-currency))
          {:keys [status body]} (http/get url {:socket-timeout 5000 :conn-timeout 3000 :as :json})]
      (when (= 200 status)
        {:BTC (get-in body ["bitcoin" (name base-currency)])
         :ETH (get-in body ["ethereum" (name base-currency)])}))
    (catch Exception e
      (println (str "[currency] Failed to fetch crypto prices: " (.getMessage e)))
      nil)))

(defn- rates-stale?
  "Check if cached rates are older than 6 hours."
  [cached-at]
  (if-not cached-at
    true
    (let [six-hours-ms (* 6 60 60 1000)]
      (> (- (System/currentTimeMillis) (.getTime cached-at)) six-hours-ms))))

(defn get-cached-rates
  "Get cached exchange rates from Datahike. Returns {rate-id ...} or nil."
  []
  (let [conn (conn/get-conn)]
    (first (d/q '[:find (max ?ts) ?base ?target ?rate
                  :where [?e :rate/timestamp ?ts]
                         [?e :rate/base-currency ?base]
                         [?e :rate/target-currency ?target]
                         [?e :rate/rate ?rate]]
                @conn))))

(defn refresh-rates!
  "Fetch and cache fresh exchange rates."
  [base-currency]
  (let [conn (conn/get-conn)
        fiat-rates (fetch-fiat-rates base-currency)
        crypto-prices (fetch-crypto-prices base-currency)
        now (java.util.Date.)
        tx-data (atom [])]
    ;; Store fiat rates
    (when fiat-rates
      (doseq [[currency rate] fiat-rates]
        (swap! tx-data conj {:db/id -1
                             :rate/base-currency (keyword base-currency)
                             :rate/target-currency (keyword (str/lower-case currency))
                             :rate/rate (double rate)
                             :rate/timestamp now})))
    ;; Store crypto prices
    (when crypto-prices
      (doseq [[asset price] crypto-prices]
        (when price
          (swap! tx-data conj {:db/id -1
                               :rate/base-currency (keyword base-currency)
                               :rate/target-currency asset
                               :rate/rate (double price)
                               :rate/timestamp now}))))
    (when (seq @tx-data)
      @(d/transact conn @tx-data))
    (println (str "[currency] Refreshed " (count @tx-data) " exchange rates for " base-currency))
    @tx-data))

(defn convert
  "Convert an amount from one currency to another. Uses cached rates, refreshes if stale."
  [amount from-currency to-currency]
  (if (= from-currency to-currency)
    amount
    (let [conn (conn/get-conn)
          rates (d/q '[:find ?target ?rate
                       :in $ ?base
                       :where [?e :rate/base-currency ?base]
                              [?e :rate/target-currency ?target]
                              [?e :rate/rate ?rate]
                              [?e :rate/timestamp ?ts]]
                     @conn (keyword from-currency))
          rate-map (into {} rates)
          rate (get rate-map (keyword to-currency))]
      (if rate
        (.multiply amount (bigdec rate) math-context)
        (do
          (println (str "[currency] No cached rate for " from-currency " -> " to-currency ". Try refreshing rates."))
          nil)))))
