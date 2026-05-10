(ns ren-finance.currency.rates
  (:require [datahike.api :as d]
            [ren-finance.db.conn :as conn]))

(defn get-rate
  "Get exchange rate from cache. Returns rate Double or nil."
  [base-currency target-currency]
  (let [conn (conn/get-conn)]
    (-> (d/q '[:find ?rate
               :in $ ?base ?target
               :where [?e :rate/base-currency ?base]
                      [?e :rate/target-currency ?target]
                      [?e :rate/rate ?rate]]
             @conn (keyword base-currency) (keyword target-currency))
        ffirst)))

(defn get-all-rates
  "Get all cached exchange rates. Returns [{:base :target :rate :timestamp}]."
  []
  (let [conn (conn/get-conn)]
    (d/q '[:find ?base ?target ?rate ?ts
           :where [?e :rate/base-currency ?base]
                  [?e :rate/target-currency ?target]
                  [?e :rate/rate ?rate]
                  [?e :rate/timestamp ?ts]]
         @conn)))

(defn get-last-refresh-time
  "Get timestamp of most recent rate refresh."
  []
  (let [conn (conn/get-conn)]
    (-> (d/q '[:find (max ?ts)
               :where [?e :rate/timestamp ?ts]]
             @conn)
        ffirst)))
