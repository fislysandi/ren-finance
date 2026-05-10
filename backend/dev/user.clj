(require '[ren-finance.db.conn :as conn])
(require '[ren-finance.server.core :as server])

(defonce system (atom nil))

(defn start []
  (println "Starting ren-finance system...")
  ;; DB + server will start when core.clj is implemented
  (println "System started. Use (reset) to reload."))

(defn stop []
  (println "Stopping system...")
  (reset! system nil)
  (println "System stopped."))

(defn reset []
  (stop) (start))

(println "ren-finance REPL loaded. Available: (start), (stop), (reset)")
