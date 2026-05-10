(ns ren-finance.server.core
  (:require [aleph.http :as http]
            [ren-finance.server.routes :as routes]
            [ren-finance.db.conn :as conn]
            [clojure.tools.logging :as log])
  )

(defonce server-atom (atom nil))
(defonce start-time-atom (atom nil))

(defn start-server
  "Start Aleph HTTP server on configured port. Default 3000."
  [& {:keys [port] :or {port (or (some-> (System/getenv "REN_HTTP_PORT") Integer/parseInt) 3000)}}]
  (log/info "Starting HTTP server on port" port)
  (reset! start-time-atom (System/currentTimeMillis))
  (let [handler (routes/route-dispatcher)
        server (http/start-server handler {:port port})]
    (reset! server-atom server)
    (log/info "HTTP server started on port" port)
    server))

(defn stop-server
  "Gracefully stop the HTTP server."
  []
  (when-let [server @server-atom]
    (log/info "Stopping HTTP server...")
    (.close server 10000)  ;; 10s timeout for in-flight
    (reset! server-atom nil)
    (log/info "HTTP server stopped")))

(defn uptime-seconds
  "Seconds since server started."
  []
  (if-let [start @start-time-atom]
    (quot (- (System/currentTimeMillis) start) 1000)
    0))

(defn -main
  "System entry point."
  [& _args]
  (try
    (log/info "=== ren-finance starting ===")
    (conn/connect)
    (start-server)
    ;; Register shutdown hook
    (.addShutdownHook (Runtime/getRuntime) (Thread. ^Runnable
      (fn []
        (log/info "Shutdown hook triggered")
        (stop-server)
        (conn/disconnect)
        (log/info "Shutdown complete"))))
    ;; Keep alive
    @(promise)
    (catch Exception e
      (log/error e "Failed to start system")
      (System/exit 1))))
