(ns ren-finance.db.conn
  (:require [datahike.api :as d]
            [ren-finance.db.schema :as schema]))

(defonce conn-atom (atom nil))

(def db-config
  {:store {:backend :file
           :path (or (System/getenv "REN_STORAGE_PATH") "./data/ren-finance")}
   :keep-history? true
   :schema-flexibility :read
   :initial-data (concat schema/all-schema)})

(defn create-db!
  "Create database if it doesn't exist, returns true if created."
  []
  (when-not (d/database-exists? db-config)
    (d/create-database db-config)
    true))

(defn connect
  "Connect to database. Creates if not exists."
  []
  (create-db!)
  (let [conn (d/connect db-config)]
    (reset! conn-atom conn)
    conn))

(defn disconnect
  "Release database connection."
  []
  (when-let [conn @conn-atom]
    (d/release conn)
    (reset! conn-atom nil)))

(defn get-conn
  "Get current connection, connecting if needed."
  []
  (if-let [conn @conn-atom]
    conn
    (connect)))
