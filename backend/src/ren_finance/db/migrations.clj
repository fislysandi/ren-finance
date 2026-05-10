(ns ren-finance.db.migrations
  (:require [datahike.api :as d]
            [clojure.edn :as edn]))

(defn load-migration
  "Load a migration EDN file from resources/migrations/"
  [filename]
  (let [path (str "resources/migrations/" filename)]
    (when (.exists (java.io.File. path))
      (edn/read-string (slurp path)))))

(defn run-migrations!
  "Run pending migrations."
  [conn]
  (println "Running migrations...")
  ;; Seed default categories on initial setup
  (let [default-categories
        [{:db/id -1 :category/name "Salary" :category/type :income :category/color "#00a87e"}
         {:db/id -2 :category/name "Freelance" :category/type :income :category/color "#00a87e"}
         {:db/id -3 :category/name "Investment" :category/type :income :category/color "#494fdf"}
         {:db/id -4 :category/name "Housing" :category/type :expense :category/color "#e23b4a"}
         {:db/id -5 :category/name "Food" :category/type :expense :category/color "#ec7e00"}
         {:db/id -6 :category/name "Transport" :category/type :expense :category/color "#494fdf"}
         {:db/id -7 :category/name "Shopping" :category/type :expense :category/color "#e23b4a"}
         {:db/id -8 :category/name "Entertainment" :category/type :expense :category/color "#00a87e"}
         {:db/id -9 :category/name "Bills" :category/type :expense :category/color "#e23b4a"}
         {:db/id -10 :category/name "Other" :category/type :expense :category/color "#ffffff"}]]
    (try
      @(d/transact conn default-categories)
      (println "  Seeded" (count default-categories) "default categories")
      (catch Exception e
        (println "  Categories already seeded or error:" (.getMessage e)))))
  (println "Migrations complete."))
