(ns ren-finance.server.routes
  (:require [reitit.ring :as ring]
            [ren-finance.server.middleware :as middleware]
            [ren-finance.db.conn :as conn]
            [ren-finance.queries.net-worth :as net-worth]
            [ren-finance.queries.transactions :as tx-queries]
            [ren-finance.import.csv :as csv-import]
            [ren-finance.wallets.ethereum :as eth]
            [ren-finance.wallets.bitcoin :as btc]
            [datahike.api :as d]
            [clojure.edn :as edn]))

(defn- json-response
  [data status]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body {:data data}})

(defn- error-response
  [code message status]
  {:status status
   :headers {"Content-Type" "application/json"}
   :body {:error {:code code :message message}}})

(def routes
  ["/api"
   ;; Health check (also handled in middleware.wrap-health)
   ["/health"
    {:get (fn [_]
            {:status 200
             :headers {"Content-Type" "application/json"}
             :body {:data {:status "ok" :db-connected true}}})}]

   ;; Net worth
   ["/net-worth"
    {:get (fn [_]
            (try
              (let [nw (net-worth/net-worth (conn/get-conn))
                    by-type (net-worth/net-worth-by-type (conn/get-conn))]
                (json-response {:total nw :by-type by-type :last-sync (java.util.Date.)} 200))
              (catch Exception e
                (error-response :internal-error (.getMessage e) 500))))}]

   ;; Accounts
   ["/accounts"
    {:get (fn [_]
            (try
              (let [accounts (net-worth/all-accounts (conn/get-conn))]
                (json-response accounts 200))
              (catch Exception e
                (error-response :internal-error (.getMessage e) 500))))}]

   ;; Transactions
   ["/transactions"
    {:get (fn [{:keys [params]}]
            (try
              (let [limit (or (some-> params :limit Integer/parseInt) 20)
                    offset (or (some-> params :offset Integer/parseInt) 0)
                    from (some-> params :from)
                    to (some-> params :to)
                    category (some-> params :category keyword)
                    result (tx-queries/query-transactions (conn/get-conn) {:limit limit :offset offset :from from :to to :category category})]
                (json-response result 200))
              (catch Exception e
                (error-response :internal-error (.getMessage e) 500))))}]

   ;; CSV import
   ["/import/csv"
    {:post (fn [{:keys [multipart]}]
             (try
               (let [file (:tempfile (first (vals multipart)))
                     mapping (some-> (:mapping multipart) edn/read-string)
                     result (csv-import/import-csv (.getAbsolutePath file) (or mapping {}))]
                 (json-response result 200))
               (catch Exception e
                 (error-response :import-error (.getMessage e) 422))))}]

   ;; Wallet sync
   ["/wallets/sync"
    {:post (fn [{:keys [body]}]
             (try
               (let [addresses (:addresses body)
                     results (doall
                               (map (fn [addr]
                                      (try
                                        (let [wallet (if (.startsWith addr "0x")
                                                       (eth/make-wallet addr)
                                                       (btc/make-wallet addr))]
                                          (.fetch-balance wallet))
                                        (catch Exception e
                                          {:address addr :error (.getMessage e) :balance 0M})))
                                    addresses))]
                 (json-response {:synced (count results) :results results} 200))
               (catch Exception e
                 (error-response :internal-error (.getMessage e) 500))))}]

   ;; Settings
   ["/settings"
    {:get (fn [_]
            (try
              (let [db @(conn/get-conn)
                    base-currency (ffirst (d/q '[:find ?v :where [?e :currency/default ?v]] db))
                    wallets (d/q '[:find ?addr ?chain ?label ?bal
                                   :where [?e :wallet/address ?addr]
                                          [?e :wallet/chain-type ?chain]
                                          [?e :wallet/label ?label]
                                          [?e :wallet/last-balance ?bal]]
                                 db)]
                (json-response {:base-currency (or base-currency :USD)
                                :wallets (mapv (fn [[addr chain label bal]]
                                                 {:address addr :chain-type chain :label label :balance bal})
                                               wallets)} 200))
              (catch Exception e
                (error-response :internal-error (.getMessage e) 500))))}]

   ;; Base currency setting
   ["/settings/base-currency"
    {:put (fn [{:keys [body]}]
            (try
              (let [conn (conn/get-conn)
                    currency (-> body :currency keyword)]
                @(d/transact conn [{:db/id -1 :currency/default currency}])
                (json-response {:currency currency} 200))
              (catch Exception e
                (error-response :validation-error (.getMessage e) 422))))}]

   ;; Wallet CRUD
   ["/settings/wallets"
    {:get (fn [_]
            (try
              (let [wallets (d/q '[:find ?addr ?chain ?label ?bal
                                   :where [?e :wallet/address ?addr]
                                          [?e :wallet/chain-type ?chain]
                                          [?e :wallet/label ?label]
                                          [?e :wallet/last-balance ?bal]]
                                 @(conn/get-conn))]
                (json-response (mapv (fn [[addr chain label bal]]
                                       {:address addr :chain-type chain :label label :balance bal})
                                     wallets) 200))
              (catch Exception e
                (error-response :internal-error (.getMessage e) 500))))
     :put (fn [{:keys [body]}]
            (try
              (let [conn (conn/get-conn)]
                @(d/transact conn [{:db/id -1
                                    :wallet/address (:address body)
                                    :wallet/chain-type (keyword (:chain-type body))
                                    :wallet/label (:label body)
                                    :wallet/last-balance 0M
                                    :wallet/last-sync (java.util.Date.)}])
                (json-response {:address (:address body)} 201))
              (catch Exception e
                (error-response :validation-error (.getMessage e) 422))))}]

   ;; Delete wallet
   ["/settings/wallets/:address"
    {:delete (fn [{:keys [path-params]}]
               (try
                 (let [conn (conn/get-conn)
                       addr (:address path-params)
                       result (d/q '[:find ?e :in $ ?addr :where [?e :wallet/address ?addr]]
                                   @conn addr)
                       eid (ffirst result)]
                   (if eid
                     (do
                       @(d/transact conn [[:db/retractEntity eid]])
                       (json-response {:deleted addr} 200))
                     (error-response :not-found "Wallet not found" 404)))
                 (catch Exception e
                   (error-response :internal-error (.getMessage e) 500))))}]])

(defn route-dispatcher
  "Create Ring handler from routes."
  []
  (-> routes
      ring/ring-handler
      middleware/app-middleware))
