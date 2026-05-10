(ns ren-finance.db.schema)

(def account-schema
  [{:db/ident :account/name, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}
   {:db/ident :account/type, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :account/balance, :db/valueType :db.type/bigdec, :db/cardinality :db.cardinality/one}
   {:db/ident :account/currency, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :account/opened-date, :db/valueType :db.type/instant, :db/cardinality :db.cardinality/one}])

(def transaction-schema
  [{:db/ident :transaction/date, :db/valueType :db.type/instant, :db/cardinality :db.cardinality/one, :db/index true}
   {:db/ident :transaction/amount, :db/valueType :db.type/bigdec, :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/description, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/account, :db/valueType :db.type/ref, :db/cardinality :db.cardinality/one, :db/index true}
   {:db/ident :transaction/category, :db/valueType :db.type/ref, :db/cardinality :db.cardinality/one, :db/index true}
   {:db/ident :transaction/type, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :transaction/import-hash, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one, :db/unique :db.unique/identity}
   {:db/ident :transaction/import-source, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}])

(def category-schema
  [{:db/ident :category/name, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}
   {:db/ident :category/type, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :category/parent, :db/valueType :db.type/ref, :db/cardinality :db.cardinality/one}
   {:db/ident :category/color, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}])

(def wallet-schema
  [{:db/ident :wallet/address, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one, :db/unique :db.unique/identity}
   {:db/ident :wallet/chain-type, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :wallet/label, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}
   {:db/ident :wallet/last-balance, :db/valueType :db.type/bigdec, :db/cardinality :db.cardinality/one}
   {:db/ident :wallet/last-sync, :db/valueType :db.type/instant, :db/cardinality :db.cardinality/one}])

(def exchange-rate-schema
  [{:db/ident :rate/base-currency, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :rate/target-currency, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}
   {:db/ident :rate/rate, :db/valueType :db.type/double, :db/cardinality :db.cardinality/one}
   {:db/ident :rate/timestamp, :db/valueType :db.type/instant, :db/cardinality :db.cardinality/one}])

(def payee-schema
  [{:db/ident :payee/name, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}])

(def budget-schema
  [{:db/ident :budget/category, :db/valueType :db.type/ref, :db/cardinality :db.cardinality/one}
   {:db/ident :budget/amount, :db/valueType :db.type/bigdec, :db/cardinality :db.cardinality/one}
   {:db/ident :budget/period, :db/valueType :db.type/keyword, :db/cardinality :db.cardinality/one}])

(def finance-schema
  (concat account-schema transaction-schema category-schema wallet-schema exchange-rate-schema payee-schema budget-schema))

(def csv-mapping-schema
  [{:db/ident :csv-mapping/name, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}
   {:db/ident :csv-mapping/columns, :db/valueType :db.type/string, :db/cardinality :db.cardinality/one}])

(def all-schema (concat finance-schema csv-mapping-schema))
