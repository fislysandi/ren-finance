(ns ren-finance.import.dedup
  (:require [datahike.api :as d]
            [ren-finance.db.conn :as conn]))

(defn find-imported-hashes
  "Return set of all existing import-hash values in DB."
  []
  (let [conn (conn/get-conn)]
    (->> (d/q '[:find ?hash
                :where [?e :transaction/import-hash ?hash]]
              @conn)
         (map first)
         (into #{}))))

(defn import-exists?
  "Check if a specific import-hash already exists in DB.
   Returns logical true if found, nil otherwise."
  [import-hash]
  (let [conn (conn/get-conn)]
    (seq (d/q '[:find ?e
                :in $ ?hash
                :where [?e :transaction/import-hash ?hash]]
              @conn import-hash))))
