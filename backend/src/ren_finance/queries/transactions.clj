(ns ren-finance.queries.transactions
  (:require [datahike.api :as d]
            [ren-finance.db.conn :as conn]))

(defn query-transactions
  "Query transactions with optional filters.
   Options: {:limit int :offset int :from inst :to inst :category keyword|string}
   Returns {:items [transaction-maps] :total int}"
  ([]
   (query-transactions (conn/get-conn) {}))
  ([conn opts]
   (let [limit   (or (:limit opts) 20)
         offset  (or (:offset opts) 0)
         from    (:from opts)
         to      (:to opts)
         cat     (:category opts)
         cat-str (when cat
                   (if (keyword? cat) (name cat) (str cat)))

         base-query '[:find ?date ?amount ?desc ?type ?cat-name
                      :where [?t :transaction/date ?date]
                             [?t :transaction/amount ?amount]
                             [?t :transaction/description ?desc]
                             [?t :transaction/type ?type]
                             [?t :transaction/category ?c]
                             [?c :category/name ?cat-name]]

         all-results (d/q base-query @conn)

         ;; Apply in-memory filters via cond->>
         filtered (cond->> all-results
                    cat-str
                    (filter (fn [[_ _ _ _ c]]
                              (= c cat-str)))

                    (and from to)
                    (filter (fn [[d]]
                              (let [ts (.getTime d)]
                                (and (>= ts (.getTime from))
                                     (<= ts (.getTime to)))))))

         total (count filtered)

         items (->> filtered
                    (sort-by first #(compare %2 %1))  ;; newest first
                    (drop offset)
                    (take limit)
                    (mapv (fn [[date amount desc type cat-name]]
                            {:date date :amount amount :description desc :type type :category cat-name})))]

     (println (str "[queries] Found " total " transactions"))
     {:items items :total total})))
